package system.solutions.com.api_sunat.sunat;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class SunatCdrProcessor {

    private static final int MAX_CDR_XML_BYTES = 10 * 1024 * 1024;

    public SunatReceipt procesar(byte[] cdrZip) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(cdrZip))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
                    byte[] xml = readLimited(zip);
                    Document document = parse(xml);
                    String code = text(document, "ResponseCode");
                    if (code == null || code.isBlank()) {
                        throw new SunatIntegrationException("El CDR recibido no contiene el código de respuesta SUNAT.");
                    }
                    String description = text(document, "Description");
                    var noteNodes = document.getElementsByTagNameNS("*", "Note");
                    StringBuilder notes = new StringBuilder();
                    for (int i = 0; i < noteNodes.getLength(); i++) {
                        if (!notes.isEmpty()) {
                            notes.append(" | ");
                        }
                        notes.append(noteNodes.item(i).getTextContent().trim());
                    }
                    return new SunatReceipt(code, description, notes.toString());
                }
            }
            throw new SunatIntegrationException("El ZIP CDR de SUNAT no contiene archivo XML.");
        } catch (SunatIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudo procesar el CDR ZIP de SUNAT.", exception);
        }
    }

    private byte[] readLimited(ZipInputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_CDR_XML_BYTES) {
                throw new SunatIntegrationException("El XML del CDR excede el tamaño permitido.");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
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

    private String text(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }
}
