package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import system.solutions.com.api_sunat.entity.Tenant;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class XmlDigitalSigner {

    private final SunatProperties properties;

    public SignedXml firmar(byte[] unsignedXml, Tenant tenant) {
        try {
            SunatProperties.TenantCredentials credentials = tenantCredentials(tenant);
            if (tenant.getCertificadoDigitalPath() == null || tenant.getCertificadoDigitalPath().isBlank()) {
                throw new SunatIntegrationException("No hay certificado digital configurado para la empresa.");
            }
            Path certRoot = properties.getCertificateRoot().toRealPath();
            Path configuredCertificate = Path.of(tenant.getCertificadoDigitalPath());
            Path certificate = (configuredCertificate.isAbsolute()
                    ? configuredCertificate
                    : certRoot.resolve(configuredCertificate)).toRealPath();
            if (!certificate.startsWith(certRoot) || !Files.isRegularFile(certificate)) {
                throw new SunatIntegrationException("El certificado del tenant debe estar dentro del almacén autorizado.");
            }

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (var input = Files.newInputStream(certificate)) {
                keyStore.load(input, credentials.getCertificatePassword().toCharArray());
            }
            String alias = null;
            var aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String candidate = aliases.nextElement();
                if (keyStore.isKeyEntry(candidate)) {
                    alias = candidate;
                    break;
                }
            }
            if (alias == null) {
                throw new SunatIntegrationException("El PKCS#12 no contiene una entrada de clave privada.");
            }
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, credentials.getCertificatePassword().toCharArray());
            X509Certificate signingCertificate = (X509Certificate) keyStore.getCertificate(alias);
            if (privateKey == null || signingCertificate == null || !"RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
                throw new SunatIntegrationException("El PKCS#12 debe contener una clave privada RSA y su certificado.");
            }
            try {
                signingCertificate.checkValidity();
            } catch (CertificateExpiredException | java.security.cert.CertificateNotYetValidException exception) {
                throw new SunatIntegrationException("El certificado digital de la empresa no está vigente.", exception);
            }

            Document document = parse(unsignedXml);
            Element extensionContent = (Element) document.getElementsByTagNameNS(
                    UblXmlGenerator.EXT, "ExtensionContent").item(0);
            if (extensionContent == null) {
                throw new SunatIntegrationException("El XML UBL no contiene ext:ExtensionContent para la firma.");
            }

            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            var transforms = List.of(
                    signatureFactory.newTransform(Transform.ENVELOPED, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                    signatureFactory.newTransform(CanonicalizationMethod.INCLUSIVE,
                            (javax.xml.crypto.dsig.spec.TransformParameterSpec) null));
            var reference = signatureFactory.newReference("", signatureFactory.newDigestMethod(DigestMethod.SHA256, null),
                    transforms, null, null);
            var signedInfo = signatureFactory.newSignedInfo(
                    signatureFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                            (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null),
                    signatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    List.of(reference));
            KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
            X509Data x509Data = keyInfoFactory.newX509Data(List.of(signingCertificate));
            var keyInfo = keyInfoFactory.newKeyInfo(List.of(x509Data));
            var signature = signatureFactory.newXMLSignature(signedInfo, keyInfo, null, "SignatureSP", null);
            DOMSignContext signContext = new DOMSignContext(privateKey, extensionContent);
            signContext.setDefaultNamespacePrefix("ds");
            signature.sign(signContext);

            Element signatureElement = (Element) document.getElementsByTagNameNS(
                    javax.xml.crypto.dsig.XMLSignature.XMLNS, "Signature").item(0);
            DOMValidateContext validationContext = new DOMValidateContext(signingCertificate.getPublicKey(), signatureElement);
            validationContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
            if (!signatureFactory.unmarshalXMLSignature(validationContext).validate(validationContext)) {
                throw new SunatIntegrationException("La firma XMLDSig no pasó la verificación criptográfica local.");
            }
            String digest = document.getElementsByTagNameNS(
                    javax.xml.crypto.dsig.XMLSignature.XMLNS, "DigestValue").item(0).getTextContent();
            return new SignedXml(serialize(document), digest);
        } catch (SunatIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SunatIntegrationException("Falló la firma XMLDSig del comprobante.", exception);
        }
    }

    private SunatProperties.TenantCredentials tenantCredentials(Tenant tenant) {
        SunatProperties.TenantCredentials credentials = properties.getTenants().get(tenant.getId());
        if (credentials == null || credentials.getCertificatePassword() == null
                || credentials.getCertificatePassword().isBlank()) {
            throw new SunatIntegrationException("No se configuró de forma segura la clave del certificado para el tenant.");
        }
        return credentials;
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml)));
    }

    private byte[] serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    public record SignedXml(byte[] content, String digestValue) {
    }
}
