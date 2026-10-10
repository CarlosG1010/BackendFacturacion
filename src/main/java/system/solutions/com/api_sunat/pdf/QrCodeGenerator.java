package system.solutions.com.api_sunat.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Map;

@Component
public class QrCodeGenerator {

    public BufferedImage generate(String payload, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    size,
                    size,
                    Map.of(
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 1,
                            EncodeHintType.CHARACTER_SET, "UTF-8"
                    )
            );
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el código QR del comprobante.", exception);
        }
    }
}
