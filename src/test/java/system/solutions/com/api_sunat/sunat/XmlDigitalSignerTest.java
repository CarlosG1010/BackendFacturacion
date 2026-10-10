package system.solutions.com.api_sunat.sunat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import system.solutions.com.api_sunat.entity.Tenant;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlDigitalSignerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void firmaYVerificaXmlConCertificadoPkcs12() throws Exception {
        Path certificateRoot = tempDirectory.resolve("certificates");
        java.nio.file.Files.createDirectories(certificateRoot);
        Path certificate = certificateRoot.resolve("tenant.p12");
        String password = "test-only-password";
        Path keytool = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "keytool.exe" : "keytool");
        Process process = new ProcessBuilder(
                keytool.toString(), "-genkeypair", "-alias", "sunat-test", "-keyalg", "RSA",
                "-keysize", "2048", "-storetype", "PKCS12", "-keystore", certificate.toString(),
                "-storepass", password, "-keypass", password, "-dname", "CN=SUNAT Test",
                "-validity", "365")
                .redirectErrorStream(true)
                .start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException(new String(process.getInputStream().readAllBytes()));
        }

        UUID tenantId = UUID.randomUUID();
        SunatProperties properties = new SunatProperties();
        properties.setCertificateRoot(certificateRoot);
        var credentials = new SunatProperties.TenantCredentials();
        credentials.setCertificatePassword(password);
        var tenantCredentials = new HashMap<UUID, SunatProperties.TenantCredentials>();
        tenantCredentials.put(tenantId, credentials);
        properties.setTenants(tenantCredentials);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCertificadoDigitalPath("tenant.p12");
        byte[] unsignedXml = """
                <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
                  xmlns:ext="urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2">
                  <ext:UBLExtensions><ext:UBLExtension><ext:ExtensionContent/></ext:UBLExtension></ext:UBLExtensions>
                </Invoice>
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        XmlDigitalSigner.SignedXml signed = new XmlDigitalSigner(properties).firmar(unsignedXml, tenant);
        Document document = parse(signed.content());
        Element signature = (Element) document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = java.nio.file.Files.newInputStream(certificate)) {
            keyStore.load(input, password.toCharArray());
        }
        X509Certificate certificateObject = (X509Certificate) keyStore.getCertificate("sunat-test");
        var validation = new DOMValidateContext(certificateObject.getPublicKey(), signature);
        XMLSignature xmlSignature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(validation);

        assertTrue(xmlSignature.validate(validation));
        assertEquals(document.getElementsByTagNameNS(XMLSignature.XMLNS, "DigestValue").item(0).getTextContent(),
                signed.digestValue());
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml)));
    }
}
