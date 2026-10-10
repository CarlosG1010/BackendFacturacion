package system.solutions.com.api_sunat.pdf;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrCodeGeneratorTest {

    @Test
    void generaCodigoQrLegibleConLosCamposSeparadosPorTuberia() throws Exception {
        String payload = "20123456789|01|F001|42|18.00|118.00|2026-10-10|6|20123456789|abc123";
        var image = new QrCodeGenerator().generate(payload, 240);

        String decoded = new MultiFormatReader().decode(
                new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();

        assertEquals(payload, decoded);
    }
}
