package system.solutions.com.api_sunat.sunat;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.ComprobanteDetalle;
import system.solutions.com.api_sunat.entity.Producto;
import system.solutions.com.api_sunat.entity.Tenant;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Component
public class UblXmlGenerator {

    static final String INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    static final String CREDIT_NOTE = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
    static final String DEBIT_NOTE = "urn:oasis:names:specification:ubl:schema:xsd:DebitNote-2";
    static final String CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    private static final BigDecimal IGV_RATE = new BigDecimal("18");
    private static final DateTimeFormatter ISSUE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] generar(Comprobante comprobante, Tenant tenant) {
        try {
            validarDatos(comprobante, tenant);
            Document document = newDocument();
            String rootName = rootName(comprobante.getTipoComprobante());
            Element root = document.createElementNS(namespace(comprobante.getTipoComprobante()), rootName);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT);
            document.appendChild(root);

            Element extensions = child(document, root, EXT, "ext:UBLExtensions");
            Element extension = child(document, extensions, EXT, "ext:UBLExtension");
            child(document, extension, EXT, "ext:ExtensionContent");

            basic(document, root, "cbc:UBLVersionID", "2.1");
            basic(document, root, "cbc:CustomizationID", "2.0");
            basic(document, root, "cbc:ID", comprobante.getSerie() + "-" + comprobante.getNumero());
            basic(document, root, "cbc:IssueDate", comprobante.getFechaEmision().toLocalDate().toString());
            basic(document, root, "cbc:IssueTime", comprobante.getFechaEmision().toLocalTime().format(ISSUE_TIME));

            if ("07".equals(comprobante.getTipoComprobante()) || "08".equals(comprobante.getTipoComprobante())) {
                addNoteTypeCode(document, root, comprobante);
            } else {
                Element typeCode = basic(document, root, "cbc:InvoiceTypeCode", comprobante.getTipoComprobante());
                typeCode.setAttribute("listID", "0101");
                typeCode.setAttribute("listAgencyName", "PE:SUNAT");
                typeCode.setAttribute("listName", "Tipo de Documento");
            }
            basic(document, root, "cbc:DocumentCurrencyCode", comprobante.getMoneda());
            if ("USD".equals(comprobante.getMoneda())) {
                Element exchange = child(document, root, CAC, "cac:PricingExchangeRate");
                basic(document, exchange, "cbc:SourceCurrencyCode", "PEN");
                basic(document, exchange, "cbc:SourceCurrencyBaseRate", "1.000000");
                basic(document, exchange, "cbc:TargetCurrencyCode", "USD");
                basic(document, exchange, "cbc:TargetCurrencyBaseRate", "1.000000");
                basic(document, exchange, "cbc:CalculationRate", comprobante.getTipoCambio().setScale(6, RoundingMode.HALF_UP).toPlainString());
                basic(document, exchange, "cbc:Date", comprobante.getFechaEmision().toLocalDate().toString());
            }
            if ("07".equals(comprobante.getTipoComprobante()) || "08".equals(comprobante.getTipoComprobante())) {
                addNoteReferences(document, root, comprobante);
            }
            addSignatureMetadata(document, root, tenant, comprobante);
            addSupplier(document, root, tenant);
            addCustomer(document, root, comprobante.getCliente());

            addTaxTotal(document, root, comprobante);
            addMonetaryTotal(document, root, comprobante);
            addLines(document, root, comprobante);
            return serialize(document);
        } catch (SunatIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudo generar el XML UBL 2.1 del comprobante.", exception);
        }
    }

    private void validarDatos(Comprobante comprobante, Tenant tenant) {
        if (tenant.getRuc() == null || !tenant.getRuc().matches("\\d{11}")) {
            throw new SunatIntegrationException("El RUC emisor debe contener 11 dígitos.");
        }
        if (tenant.getRazonSocial() == null || tenant.getRazonSocial().isBlank()
                || tenant.getDireccionFiscal() == null || tenant.getDireccionFiscal().isBlank()) {
            throw new SunatIntegrationException("El emisor debe tener razón social y dirección fiscal.");
        }
        if (comprobante.getCliente().getNombreRazonSocial() == null
                || comprobante.getCliente().getNombreRazonSocial().isBlank()) {
            throw new SunatIntegrationException("El cliente debe tener nombre o razón social.");
        }
        if (comprobante.getCliente().getDireccion() == null || comprobante.getCliente().getDireccion().isBlank()) {
            throw new SunatIntegrationException("El cliente debe tener dirección registrada para emitir UBL.");
        }
        if (comprobante.getDetalles() == null || comprobante.getDetalles().isEmpty()) {
            throw new SunatIntegrationException("El comprobante debe incluir al menos un detalle.");
        }
        if (("07".equals(comprobante.getTipoComprobante()) || "08".equals(comprobante.getTipoComprobante()))
                && (comprobante.getComprobanteOrigen() == null
                || !"ACEPTADO".equals(comprobante.getComprobanteOrigen().getEstadoSunat()))) {
            throw new SunatIntegrationException("La nota solo puede referenciar un comprobante previamente aceptado por SUNAT.");
        }
        if (("07".equals(comprobante.getTipoComprobante()) || "08".equals(comprobante.getTipoComprobante()))
                && (comprobante.getComprobanteOrigen() == null
                || comprobante.getMotivoNotaCodigo() == null || comprobante.getMotivoNota() == null
                || comprobante.getMotivoNota().isBlank())) {
            throw new SunatIntegrationException("Las notas requieren comprobante origen y motivo.");
        }
        if ("01".equals(comprobante.getTipoComprobante())
                && !"RUC".equalsIgnoreCase(comprobante.getCliente().getTipoDocumento())) {
            throw new SunatIntegrationException("Una factura requiere cliente identificado con RUC.");
        }
        if ("07".equals(comprobante.getTipoComprobante())
                && !java.util.Set.of("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14")
                .contains(comprobante.getMotivoNotaCodigo())) {
            throw new SunatIntegrationException("El código de motivo de nota de crédito no pertenece al catálogo SUNAT.");
        }
        if ("08".equals(comprobante.getTipoComprobante())
                && !java.util.Set.of("01", "02", "03", "10").contains(comprobante.getMotivoNotaCodigo())) {
            throw new SunatIntegrationException("El código de motivo de nota de débito no pertenece al catálogo SUNAT.");
        }
    }

    private void addNoteTypeCode(Document document, Element root, Comprobante comprobante) {
        boolean creditNote = "07".equals(comprobante.getTipoComprobante());
        Element type = basic(document, root, creditNote ? "cbc:CreditNoteTypeCode" : "cbc:DebitNoteTypeCode",
                comprobante.getTipoComprobante());
        type.setAttribute("listAgencyName", "PE:SUNAT");
        type.setAttribute("listName", "Tipo de Nota");
    }

    private void addNoteReferences(Document document, Element root, Comprobante comprobante) {
        Element billingReference = child(document, root, CAC, "cac:BillingReference");
        Element invoiceDocumentReference = child(document, billingReference, CAC, "cac:InvoiceDocumentReference");
        basic(document, invoiceDocumentReference, "cbc:ID", comprobante.getComprobanteOrigen().getSerie()
                + "-" + comprobante.getComprobanteOrigen().getNumero());
        Element documentTypeCode = basic(document, invoiceDocumentReference, "cbc:DocumentTypeCode",
                comprobante.getComprobanteOrigen().getTipoComprobante());
        documentTypeCode.setAttribute("listAgencyName", "PE:SUNAT");
        documentTypeCode.setAttribute("listName", "Tipo de Documento");
        Element response = child(document, root, CAC, "cac:DiscrepancyResponse");
        basic(document, response, "cbc:ReferenceID", comprobante.getComprobanteOrigen().getSerie()
                + "-" + comprobante.getComprobanteOrigen().getNumero());
        basic(document, response, "cbc:ResponseCode", comprobante.getMotivoNotaCodigo());
        basic(document, response, "cbc:Description", comprobante.getMotivoNota());
    }

    private void addSignatureMetadata(Document document, Element root, Tenant tenant, Comprobante comprobante) {
        Element signature = child(document, root, CAC, "cac:Signature");
        basic(document, signature, "cbc:ID", "SIGN-" + comprobante.getId());
        Element party = child(document, signature, CAC, "cac:SignatoryParty");
        Element partyIdentification = child(document, party, CAC, "cac:PartyIdentification");
        basic(document, partyIdentification, "cbc:ID", tenant.getRuc());
        Element partyName = child(document, party, CAC, "cac:PartyName");
        basic(document, partyName, "cbc:Name", tenant.getRazonSocial());
        Element attachment = child(document, signature, CAC, "cac:DigitalSignatureAttachment");
        Element externalReference = child(document, attachment, CAC, "cac:ExternalReference");
        basic(document, externalReference, "cbc:URI", "#SignatureSP");
    }

    private void addSupplier(Document document, Element root, Tenant tenant) {
        Element supplier = child(document, root, CAC, "cac:AccountingSupplierParty");
        Element party = child(document, supplier, CAC, "cac:Party");
        Element identification = child(document, party, CAC, "cac:PartyIdentification");
        Element id = basic(document, identification, "cbc:ID", tenant.getRuc());
        id.setAttribute("schemeID", "6");
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        addTaxScheme(document, party, tenant.getRazonSocial(), tenant.getRuc());
        Element legal = child(document, party, CAC, "cac:PartyLegalEntity");
        basic(document, legal, "cbc:RegistrationName", tenant.getRazonSocial());
        Element address = child(document, legal, CAC, "cac:RegistrationAddress");
        basic(document, address, "cbc:AddressTypeCode", "0000");
        Element line = child(document, address, CAC, "cac:AddressLine");
        basic(document, line, "cbc:Line", tenant.getDireccionFiscal());
        Element country = child(document, address, CAC, "cac:Country");
        basic(document, country, "cbc:IdentificationCode", "PE");
    }

    private void addCustomer(Document document, Element root, Cliente cliente) {
        Element customer = child(document, root, CAC, "cac:AccountingCustomerParty");
        Element party = child(document, customer, CAC, "cac:Party");
        Element identification = child(document, party, CAC, "cac:PartyIdentification");
        Element id = basic(document, identification, "cbc:ID", cliente.getNumeroDocumento());
        id.setAttribute("schemeID", customerDocumentCode(cliente.getTipoDocumento()));
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        Element legal = child(document, party, CAC, "cac:PartyLegalEntity");
        basic(document, legal, "cbc:RegistrationName", cliente.getNombreRazonSocial());
        Element address = child(document, legal, CAC, "cac:RegistrationAddress");
        Element line = child(document, address, CAC, "cac:AddressLine");
        basic(document, line, "cbc:Line", cliente.getDireccion());
        Element country = child(document, address, CAC, "cac:Country");
        basic(document, country, "cbc:IdentificationCode", "PE");
    }

    private void addTaxScheme(Document document, Element party, String registrationName, String ruc) {
        Element taxScheme = child(document, party, CAC, "cac:PartyTaxScheme");
        basic(document, taxScheme, "cbc:RegistrationName", registrationName);
        Element companyId = basic(document, taxScheme, "cbc:CompanyID", ruc);
        companyId.setAttribute("schemeID", "6");
        companyId.setAttribute("schemeName", "SUNAT:Identificador de Documento de Identidad");
        companyId.setAttribute("schemeAgencyName", "PE:SUNAT");
        Element scheme = child(document, taxScheme, CAC, "cac:TaxScheme");
        taxSchemeId(document, scheme, "1000");
        basic(document, scheme, "cbc:Name", "IGV");
        basic(document, scheme, "cbc:TaxTypeCode", "VAT");
    }

    private void addTaxTotal(Document document, Element root, Comprobante comprobante) {
        Element total = child(document, root, CAC, "cac:TaxTotal");
        amount(document, total, "cbc:TaxAmount", comprobante.getMontoIgv(), comprobante.getMoneda());
        addInvoiceTaxSubtotal(document, total, "10", comprobante.getMontoGravado(), comprobante.getMontoIgv(), comprobante.getMoneda());
        addInvoiceTaxSubtotal(document, total, "20", comprobante.getMontoExonerado(), BigDecimal.ZERO, comprobante.getMoneda());
        addInvoiceTaxSubtotal(document, total, "30", comprobante.getMontoInafecto(), BigDecimal.ZERO, comprobante.getMoneda());
        addInvoiceTaxSubtotal(document, total, "11", comprobante.getMontoGratuito(), BigDecimal.ZERO, comprobante.getMoneda());
    }

    private void addInvoiceTaxSubtotal(Document document, Element total, String affectation,
                                       BigDecimal base, BigDecimal tax, String currency) {
        if (base == null || base.signum() == 0) {
            return;
        }
        String schemeId = taxSchemeCode(affectation);
        Element subtotal = child(document, total, CAC, "cac:TaxSubtotal");
        amount(document, subtotal, "cbc:TaxableAmount", base, currency);
        amount(document, subtotal, "cbc:TaxAmount", tax, currency);
        Element category = child(document, subtotal, CAC, "cac:TaxCategory");
        taxCategoryId(document, category);
        basic(document, category, "cbc:Percent", ("10".equals(affectation) || "11".equals(affectation))
                ? IGV_RATE.toPlainString() : "0");
        taxAffectationCode(document, category, affectation);
        Element scheme = child(document, category, CAC, "cac:TaxScheme");
        taxSchemeId(document, scheme, schemeId);
        basic(document, scheme, "cbc:Name", "1000".equals(schemeId) ? "IGV" : "EXP");
        basic(document, scheme, "cbc:TaxTypeCode", "1000".equals(schemeId) ? "VAT" : "FRE");
    }

    private void addMonetaryTotal(Document document, Element root, Comprobante comprobante) {
        Element total = child(document, root, CAC, "cac:LegalMonetaryTotal");
        BigDecimal taxable = comprobante.getMontoGravado().add(comprobante.getMontoExonerado())
                .add(comprobante.getMontoInafecto());
        amount(document, total, "cbc:LineExtensionAmount", taxable, comprobante.getMoneda());
        if (comprobante.getMontoGratuito().signum() > 0) {
            amount(document, total, "cbc:AllowanceTotalAmount", comprobante.getMontoGratuito(), comprobante.getMoneda());
        }
        amount(document, total, "cbc:TaxInclusiveAmount", comprobante.getMontoTotal(), comprobante.getMoneda());
        amount(document, total, "cbc:PayableAmount", comprobante.getMontoTotal(), comprobante.getMoneda());
    }

    private void addLines(Document document, Element root, Comprobante comprobante) {
        int lineNumber = 1;
        for (ComprobanteDetalle detalle : comprobante.getDetalles()) {
            Producto producto = detalle.getProducto();
            boolean note = "07".equals(comprobante.getTipoComprobante()) || "08".equals(comprobante.getTipoComprobante());
            String lineName = note
                    ? ("07".equals(comprobante.getTipoComprobante()) ? "cac:CreditNoteLine" : "cac:DebitNoteLine")
                    : "cac:InvoiceLine";
            Element line = child(document, root, CAC, lineName);
            basic(document, line, "cbc:ID", Integer.toString(lineNumber++));
            boolean creditNote = "07".equals(comprobante.getTipoComprobante());
            String quantityName = creditNote ? "cbc:CreditedQuantity"
                    : "08".equals(comprobante.getTipoComprobante()) ? "cbc:DebitedQuantity" : "cbc:InvoicedQuantity";
            Element quantity = basic(document, line, quantityName, detalle.getCantidad().toPlainString());
            quantity.setAttribute("unitCode", producto.getUnidadMedida());
            amount(document, line, "cbc:LineExtensionAmount", detalle.isGratuito() ? BigDecimal.ZERO : detalle.getSubtotal(),
                    comprobante.getMoneda());

            Element pricing = child(document, line, CAC, "cac:PricingReference");
            Element alternativePrice = child(document, pricing, CAC, "cac:AlternativeConditionPrice");
            BigDecimal grossUnitPrice = (detalle.isGratuito() ? detalle.getSubtotal() : detalle.getTotal())
                    .divide(detalle.getCantidad(), 4, RoundingMode.HALF_UP);
            amount(document, alternativePrice, "cbc:PriceAmount", grossUnitPrice, comprobante.getMoneda());
            Element priceType = basic(document, alternativePrice, "cbc:PriceTypeCode", detalle.isGratuito() ? "02" : "01");
            priceType.setAttribute("listName", "Tipo de Precio");
            priceType.setAttribute("listAgencyName", "PE:SUNAT");

            Element taxTotal = child(document, line, CAC, "cac:TaxTotal");
            amount(document, taxTotal, "cbc:TaxAmount", detalle.getIgv(), comprobante.getMoneda());
            Element subtotal = child(document, taxTotal, CAC, "cac:TaxSubtotal");
            amount(document, subtotal, "cbc:TaxableAmount", detalle.getSubtotal(), comprobante.getMoneda());
            amount(document, subtotal, "cbc:TaxAmount", detalle.getIgv(), comprobante.getMoneda());
            Element category = child(document, subtotal, CAC, "cac:TaxCategory");
            taxCategoryId(document, category);
            String baseAffectation = baseAffectation(detalle.getTipoAfectacionIgv());
            basic(document, category, "cbc:Percent", ("10".equals(baseAffectation))
                    ? IGV_RATE.toPlainString() : "0");
            taxAffectationCode(document, category, detalle.getTipoAfectacionIgv());
            Element taxScheme = child(document, category, CAC, "cac:TaxScheme");
            String schemeId = taxSchemeCode(detalle.getTipoAfectacionIgv());
            taxSchemeId(document, taxScheme, schemeId);
            basic(document, taxScheme, "cbc:Name", "1000".equals(schemeId) ? "IGV" : "EXP");
            basic(document, taxScheme, "cbc:TaxTypeCode", "1000".equals(schemeId) ? "VAT" : "FRE");

            Element item = child(document, line, CAC, "cac:Item");
            basic(document, item, "cbc:Description", producto.getDescripcion());
            Element sellersId = child(document, item, CAC, "cac:SellersItemIdentification");
            basic(document, sellersId, "cbc:ID", producto.getCodigoInterno());
            Element price = child(document, line, CAC, "cac:Price");
            amount(document, price, "cbc:PriceAmount",
                    detalle.getSubtotal().divide(detalle.getCantidad(), 4, RoundingMode.HALF_UP), comprobante.getMoneda());
        }
    }

    private String customerDocumentCode(String type) {
        return switch (type == null ? "" : type.toUpperCase()) {
            case "DNI" -> "1";
            case "RUC" -> "6";
            case "CE" -> "4";
            case "PASAPORTE" -> "7";
            default -> throw new SunatIntegrationException("Tipo de documento de cliente no soportado por el catálogo SUNAT.");
        };
    }

    private String rootName(String type) {
        return switch (type) {
            case "01", "03" -> "Invoice";
            case "07" -> "CreditNote";
            case "08" -> "DebitNote";
            default -> throw new SunatIntegrationException("Tipo de comprobante no soportado: " + type);
        };
    }

    private String namespace(String type) {
        return switch (type) {
            case "07" -> CREDIT_NOTE;
            case "08" -> DEBIT_NOTE;
            default -> INVOICE;
        };
    }

    private Document newDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().newDocument();
    }

    private Element basic(Document document, Element parent, String qualifiedName, String value) {
        Element element = document.createElementNS(CBC, qualifiedName);
        element.setTextContent(value == null ? "" : value);
        parent.appendChild(element);
        return element;
    }

    private Element child(Document document, Element parent, String namespace, String qualifiedName) {
        Element element = document.createElementNS(namespace, qualifiedName);
        parent.appendChild(element);
        return element;
    }

    private void amount(Document document, Element parent, String qualifiedName, BigDecimal value, String currency) {
        Element element = basic(document, parent, qualifiedName, value.setScale(2, RoundingMode.HALF_UP).toPlainString());
        element.setAttribute("currencyID", currency);
    }

    private String baseAffectation(String code) {
        return switch (code) {
            case "11", "21", "31" -> code.substring(0, 1) + "0";
            default -> code;
        };
    }

    private String taxSchemeCode(String code) {
        return switch (baseAffectation(code)) {
            case "10" -> "1000";
            case "20" -> "9997";
            case "30" -> "9998";
            default -> throw new SunatIntegrationException("Tipo de afectación IGV no soportado: " + code);
        };
    }

    private void taxCategoryId(Document document, Element category) {
        Element id = basic(document, category, "cbc:ID", "S");
        id.setAttribute("schemeID", "UN/ECE 5305");
        id.setAttribute("schemeName", "Tax Category Identifier");
        id.setAttribute("schemeAgencyName", "United Nations Economic Commission for Europe");
    }

    private void taxAffectationCode(Document document, Element category, String affectation) {
        Element code = basic(document, category, "cbc:TaxExemptionReasonCode", affectation);
        code.setAttribute("listAgencyName", "PE:SUNAT");
        code.setAttribute("listName", "Afectacion del IGV");
    }

    private void taxSchemeId(Document document, Element taxScheme, String schemeId) {
        Element id = basic(document, taxScheme, "cbc:ID", schemeId);
        id.setAttribute("schemeName", "Codigo de tributos");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
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
}
