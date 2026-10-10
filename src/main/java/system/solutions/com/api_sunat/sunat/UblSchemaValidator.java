package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class UblSchemaValidator {

    private final SunatProperties properties;

    public void validar(byte[] xml, String tipoComprobante) {
        String xsdName = switch (tipoComprobante) {
            case "01", "03" -> "UBL-Invoice-2.1.xsd";
            case "07" -> "UBL-CreditNote-2.1.xsd";
            case "08" -> "UBL-DebitNote-2.1.xsd";
            case "RESUMEN" -> "UBL-2.0-SummaryDocuments-2.0.xsd";
            case "BAJA" -> "UBL-2.0-VoidedDocuments-2.0.xsd";
            default -> throw new SunatIntegrationException("No existe XSD UBL configurado para este tipo de comprobante.");
        };
        Path schemaPath = properties.getSchemaDirectory().resolve(xsdName).normalize();
        try {
            if (!Files.isRegularFile(schemaPath)) {
                throw new SunatIntegrationException("No se encontró el XSD SUNAT requerido: " + xsdName);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            factory.newSchema(schemaPath.toFile()).newValidator()
                    .validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (SAXException exception) {
            throw new SunatIntegrationException("El XML generado no cumple el XSD UBL 2.1 de SUNAT.", exception);
        } catch (SunatIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudo validar el XML UBL contra los XSD SUNAT.", exception);
        }
    }
}
