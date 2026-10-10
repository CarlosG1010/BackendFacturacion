package system.solutions.com.api_sunat.sunat;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.ComprobanteDetalle;
import system.solutions.com.api_sunat.entity.Producto;
import system.solutions.com.api_sunat.entity.Tenant;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UblXmlGeneratorTest {

    private final UblXmlGenerator generator = new UblXmlGenerator();

    @Test
    void generaFacturaUblConCatalogosIgvYSumas() throws Exception {
        Document document = parse(generator.generar(comprobante("01"), tenant()));

        assertEquals("Invoice", document.getDocumentElement().getLocalName());
        assertEquals("2.1", text(document, "UBLVersionID"));
        assertEquals("01", text(document, "InvoiceTypeCode"));
        var taxSchemes = document.getElementsByTagNameNS(UblXmlGenerator.CAC, "TaxScheme");
        assertEquals("1000", taxSchemes.item(0).getFirstChild().getTextContent());
        assertEquals("10", text(document, "TaxExemptionReasonCode"));
        assertEquals("100.00", text(document, "LineExtensionAmount"));
        assertEquals("118.00", text(document, "PayableAmount"));
        assertEquals("NIU", document.getElementsByTagNameNS(UblXmlGenerator.CBC, "InvoicedQuantity")
                .item(0).getAttributes().getNamedItem("unitCode").getNodeValue());
    }

    @Test
    void generaNotaDeCreditoConReferenciaYMotivo() throws Exception {
        Comprobante creditNote = comprobante("07");
        Comprobante source = comprobante("01");
        source.setEstadoSunat("ACEPTADO");
        creditNote.setComprobanteOrigen(source);
        creditNote.setMotivoNotaCodigo("01");
        creditNote.setMotivoNota("Anulación de la operación");

        Document document = parse(generator.generar(creditNote, tenant()));

        assertEquals("CreditNote", document.getDocumentElement().getLocalName());
        assertEquals("07", text(document, "CreditNoteTypeCode"));
        assertEquals("F001-40", text(document, "ID"));
        assertEquals("Anulación de la operación", text(document, "Description"));
        assertEquals("F001-40", text(document, "ReferenceID"));
    }

    private Comprobante comprobante(String type) {
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento("RUC");
        cliente.setNumeroDocumento("20123456789");
        cliente.setNombreRazonSocial("Cliente de prueba S.A.C.");
        cliente.setDireccion("Av. Test 123, Lima");

        Producto product = new Producto();
        product.setCodigoInterno("SKU-1");
        product.setDescripcion("Servicio de prueba");
        product.setUnidadMedida("NIU");

        ComprobanteDetalle detail = new ComprobanteDetalle();
        detail.setProducto(product);
        detail.setCantidad(new BigDecimal("1"));
        detail.setPrecioUnitario(new BigDecimal("118.0000"));
        detail.setSubtotal(new BigDecimal("100.00"));
        detail.setIgv(new BigDecimal("18.00"));
        detail.setTotal(new BigDecimal("118.00"));

        Comprobante comprobante = new Comprobante();
        comprobante.setTipoComprobante(type);
        comprobante.setSerie("F001");
        comprobante.setNumero(40L);
        comprobante.setFechaEmision(LocalDateTime.of(2026, 10, 10, 12, 0));
        comprobante.setCliente(cliente);
        comprobante.setMontoGravado(new BigDecimal("100.00"));
        comprobante.setMontoIgv(new BigDecimal("18.00"));
        comprobante.setMontoTotal(new BigDecimal("118.00"));
        comprobante.addDetalle(detail);
        return comprobante;
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setRuc("20123456789");
        tenant.setRazonSocial("Empresa Emisora S.A.C.");
        tenant.setDireccionFiscal("Jr. Emisor 456, Lima");
        return tenant;
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml)));
    }

    private String text(Document document, String localName) {
        return document.getElementsByTagNameNS(UblXmlGenerator.CBC, localName).item(0).getTextContent();
    }
}
