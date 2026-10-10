package system.solutions.com.api_sunat.sunat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SunatCdrProcessorTest {

    private final SunatCdrProcessor processor = new SunatCdrProcessor();

    @Test
    void extraeCodigoDescripcionYObservacionesDesdeElCdr() throws Exception {
        String response = """
                <ApplicationResponse xmlns="urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2"
                  xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                  xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
                  <cac:DocumentResponse><cac:Response>
                    <cbc:ResponseCode>0</cbc:ResponseCode>
                    <cbc:Description>La Factura numero F001-40 ha sido aceptada</cbc:Description>
                    <cbc:Note>Observación de prueba</cbc:Note>
                  </cac:Response></cac:DocumentResponse>
                </ApplicationResponse>
                """;
        byte[] cdrZip = zip("R-20123456789-01-F001-40.xml", response);

        SunatReceipt receipt = processor.procesar(cdrZip);

        assertEquals("0", receipt.responseCode());
        assertEquals("La Factura numero F001-40 ha sido aceptada", receipt.description());
        assertEquals("Observación de prueba", receipt.notes());
    }

    private byte[] zip(String name, String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
