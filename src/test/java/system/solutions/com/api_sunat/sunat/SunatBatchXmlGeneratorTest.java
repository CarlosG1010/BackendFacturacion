package system.solutions.com.api_sunat.sunat;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.ComprobanteDetalle;
import system.solutions.com.api_sunat.entity.Tenant;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunatBatchXmlGeneratorTest {

    private final SunatBatchXmlGenerator generator = new SunatBatchXmlGenerator();

    @Test
    void generaResumenConIdentificacionDeClienteYOperacionGratuita() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setRuc("20123456789");
        tenant.setRazonSocial("Empresa de prueba S.A.C.");

        Cliente cliente = new Cliente();
        cliente.setTipoDocumento("DNI");
        cliente.setNumeroDocumento("12345678");
        cliente.setNombreRazonSocial("Cliente de prueba");

        Comprobante comprobante = new Comprobante();
        comprobante.setTipoComprobante("03");
        comprobante.setSerie("B001");
        comprobante.setNumero(1L);
        comprobante.setFechaEmision(LocalDateTime.of(2026, 10, 9, 10, 0));
        comprobante.setCliente(cliente);
        comprobante.setMontoTotal(new BigDecimal("0.00"));
        comprobante.setMontoGravado(BigDecimal.ZERO);
        comprobante.setMontoIgv(BigDecimal.ZERO);
        comprobante.setMontoExonerado(BigDecimal.ZERO);
        comprobante.setMontoInafecto(BigDecimal.ZERO);
        comprobante.setMoneda("PEN");
        ComprobanteDetalle freeLine = new ComprobanteDetalle();
        freeLine.setGratuito(true);
        freeLine.setTipoAfectacionIgv("11");
        freeLine.setSubtotal(new BigDecimal("25.00"));
        comprobante.addDetalle(freeLine);

        var xml = generator.resumen("1", tenant, LocalDate.of(2026, 10, 9), List.of(comprobante));
        var documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var document = documentFactory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml)));
        Element root = document.getDocumentElement();

        assertEquals("SummaryDocuments", root.getLocalName());
        assertEquals("RC-20261009-1", document.getElementsByTagNameNS(UblXmlGenerator.CBC, "ID")
                .item(0).getTextContent());
        assertEquals("11", document.getElementsByTagNameNS(UblXmlGenerator.CBC, "TaxExemptionReasonCode")
                .item(1).getTextContent());
        assertNotNull(document.getElementsByTagNameNS(UblXmlGenerator.CAC, "AccountingCustomerParty").item(0));
        var schemes = document.getElementsByTagNameNS(UblXmlGenerator.CAC, "TaxScheme");
        assertTrue(schemes.item(schemes.getLength() - 1).getTextContent().contains("9996"));
    }
}
