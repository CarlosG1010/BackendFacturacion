package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import system.solutions.com.api_sunat.entity.Tenant;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class SunatSoapClient {

    private static final int MAX_SOAP_RESPONSE_BYTES = 20 * 1024 * 1024;
    private final SunatProperties properties;

    public SunatSoapResponse enviarComprobante(Tenant tenant, String fileName, byte[] zipContent) {
        SunatProperties.TenantCredentials credentials = credentials(tenant);
        String username = tenant.getRuc() + credentials.getSunatUsername();
        String body = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.sunat.gob.pe">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <ser:sendBill>
                      <fileName>%s</fileName>
                      <contentFile>%s</contentFile>
                    </ser:sendBill>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(xmlEscape(fileName), Base64.getEncoder().encodeToString(zipContent));
        byte[] response = post(body, username, credentials.getSunatPassword());
        Document document = parse(response);
        String applicationResponse = text(document, "applicationResponse");
        if (applicationResponse == null || applicationResponse.isBlank()) {
            throw soapFaultOrUnexpected(document, "SUNAT no devolvió applicationResponse.");
        }
        return new SunatSoapResponse(applicationResponse, null, null, null);
    }

    private byte[] post(String soapBody, String username, String password) {
        URI endpoint;
        try {
            endpoint = URI.create(properties.endpoint());
            if (!"https".equalsIgnoreCase(endpoint.getScheme())) {
                throw new SunatIntegrationException("El endpoint de SUNAT debe usar HTTPS.");
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(properties.getResponseTimeoutSeconds()))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "")
                    .header("Authorization", basicAuthorization(username, password))
                    .POST(HttpRequest.BodyPublishers.ofString(soapBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new SunatIntegrationException("SUNAT devolvió HTTP " + response.statusCode() + ".");
                }
                return readLimited(body);
            }
        } catch (SunatIntegrationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SunatIntegrationException("Se interrumpió la comunicación SOAP con SUNAT.", exception);
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudo conectar con el servicio SOAP de SUNAT.", exception);
        }
    }

    private byte[] readLimited(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > MAX_SOAP_RESPONSE_BYTES) {
                throw new SunatIntegrationException("La respuesta SOAP excede el tamaño permitido.");
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private SunatProperties.TenantCredentials credentials(Tenant tenant) {
        SunatProperties.TenantCredentials credentials = properties.getTenants().get(tenant.getId());
        if (credentials == null || credentials.getSunatUsername() == null || credentials.getSunatPassword() == null
                || credentials.getSunatUsername().isBlank() || credentials.getSunatPassword().isBlank()) {
            throw new SunatIntegrationException("No se configuraron credenciales SUNAT para el tenant.");
        }
        return credentials;
    }

    private String basicAuthorization(String username, String password) {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Document parse(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml)));
        } catch (Exception exception) {
            throw new SunatIntegrationException("La respuesta SOAP de SUNAT no contiene XML válido.", exception);
        }
    }

    private String text(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private SunatIntegrationException soapFaultOrUnexpected(Document document, String fallback) {
        String fault = text(document, "faultstring");
        if (fault == null) {
            fault = text(document, "Text");
        }
        return new SunatIntegrationException(fault == null || fault.isBlank() ? fallback : "Error SOAP SUNAT: " + fault);
    }

    private String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
