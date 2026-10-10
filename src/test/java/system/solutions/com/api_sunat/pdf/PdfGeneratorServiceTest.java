package system.solutions.com.api_sunat.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.ComprobanteDetalle;
import system.solutions.com.api_sunat.entity.Producto;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.sunat.SunatProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGeneratorServiceTest {

    private final PdfGeneratorService service = new PdfGeneratorService(
            new QrCodeGenerator(), new SpanishAmountWords(), new SunatProperties());

    @Test
    void generaPdfEnLosTresFormatosConLeyendasYTotales() throws Exception {
        for (PrintFormat format : PrintFormat.values()) {
            byte[] pdf = service.generar(invoice(), tenant(), format);
            String text;
            try (PdfReader reader = new PdfReader(pdf)) {
                text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");
            }

            assertTrue(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));
            assertTrue(text.contains("FACTURA"), text);
            assertTrue(text.contains("SON: CIENTO DIECIOCHO CON 00/100 SOLES"), text);
            assertTrue(text.contains("Representaci"), text);
        }
    }

    @Test
    void generaRepresentacionDeBoletaYNotas() throws Exception {
        for (String type : new String[]{"03", "07", "08"}) {
            Comprobante comprobante = invoice();
            comprobante.setTipoComprobante(type);
            try (PdfReader reader = new PdfReader(service.generar(comprobante, tenant(), PrintFormat.A4))) {
                String text = new PdfTextExtractor(reader).getTextFromPage(1);
                assertTrue(text.contains(type.equals("03") ? "BOLETA" : type.equals("07") ? "NOTA DE CR" : "NOTA DE D"));
            }
        }
    }

    private Comprobante invoice() {
        Cliente customer = new Cliente();
        customer.setTipoDocumento("RUC");
        customer.setNumeroDocumento("20987654321");
        customer.setNombreRazonSocial("Cliente de prueba S.A.C.");
        customer.setDireccion("Av. Cliente 123, Lima");

        Producto product = new Producto();
        product.setCodigoInterno("SKU-01");
        product.setDescripcion("Producto de prueba");
        product.setUnidadMedida("NIU");

        ComprobanteDetalle detail = new ComprobanteDetalle();
        detail.setProducto(product);
        detail.setCantidad(BigDecimal.ONE);
        detail.setPrecioUnitario(new BigDecimal("118.00"));
        detail.setSubtotal(new BigDecimal("100.00"));
        detail.setIgv(new BigDecimal("18.00"));
        detail.setTotal(new BigDecimal("118.00"));

        Comprobante comprobante = new Comprobante();
        comprobante.setTipoComprobante("01");
        comprobante.setSerie("F001");
        comprobante.setNumero(42L);
        comprobante.setFechaEmision(LocalDateTime.of(2026, 10, 10, 10, 30));
        comprobante.setCliente(customer);
        comprobante.setMontoGravado(new BigDecimal("100.00"));
        comprobante.setMontoIgv(new BigDecimal("18.00"));
        comprobante.setMontoTotal(new BigDecimal("118.00"));
        comprobante.setHashTributario("digest-value");
        comprobante.setEstadoSunat("ACEPTADO");
        comprobante.addDetalle(detail);
        return comprobante;
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setRuc("20123456789");
        tenant.setRazonSocial("Empresa de prueba S.A.C.");
        tenant.setDireccionFiscal("Jr. Emisor 123, Lima");
        return tenant;
    }
}
