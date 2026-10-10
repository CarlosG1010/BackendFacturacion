package system.solutions.com.api_sunat.sunat;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.Tenant;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SunatBatchXmlGenerator {

    private static final String CAC = UblXmlGenerator.CAC;
    private static final String CBC = UblXmlGenerator.CBC;
    private static final String EXT = UblXmlGenerator.EXT;
    private static final String SUMMARY_NS = "urn:oasis:names:specification:ubl:schema:xsd:SummaryDocuments-2";
    private static final String VOIDED_NS = "urn:oasis:names:specification:ubl:schema:xsd:VoidedDocuments-2";
    private static final DateTimeFormatter DATE_ID = DateTimeFormatter.BASIC_ISO_DATE;

    public byte[] resumen(String id, Tenant tenant, LocalDate issueDate, List<Comprobante> boletas) {
        Document document = create();
        Element root = root(document, SUMMARY_NS, "SummaryDocuments");
        commonHeader(document, root, "RC-" + issueDate.format(DATE_ID) + "-" + id,
                issueDate, LocalDate.now(), tenant);
        int lineNumber = 1;
        for (Comprobante comprobante : boletas) {
            Element line = child(document, root, CAC, "cac:SummaryDocumentsLine");
            basic(document, line, "cbc:LineID", Integer.toString(lineNumber++));
            basic(document, line, "cbc:DocumentTypeCode", "03");
            basic(document, line, "cbc:ID", comprobante.getSerie() + "-" + comprobante.getNumero());
            basic(document, line, "cbc:ConditionCode", "1");
            Element total = child(document, line, CAC, "cac:TotalAmount");
            amount(document, total, comprobante.getMontoTotal(), comprobante.getMoneda());
            addTax(document, line, "1000", "IGV", "10", comprobante.getMontoGravado(),
                    comprobante.getMontoIgv(), comprobante.getMoneda());
            if (comprobante.getMontoExonerado().signum() > 0) {
                addTax(document, line, "9997", "EXO", "20", comprobante.getMontoExonerado(),
                        java.math.BigDecimal.ZERO, comprobante.getMoneda());
            }
            if (comprobante.getMontoInafecto().signum() > 0) {
                addTax(document, line, "9998", "INA", "30", comprobante.getMontoInafecto(),
                        java.math.BigDecimal.ZERO, comprobante.getMoneda());
            }
            java.util.Map<String, java.math.BigDecimal> freeTotals = new java.util.HashMap<>();
            comprobante.getDetalles().stream().filter(detail -> detail.isGratuito())
                    .forEach(detail -> freeTotals.merge(detail.getTipoAfectacionIgv(), detail.getSubtotal(),
                            java.math.BigDecimal::add));
            freeTotals.forEach((affectation, base) -> addTax(document, line, "9996", "GRA", affectation,
                    base, java.math.BigDecimal.ZERO, comprobante.getMoneda()));
            Element customer = child(document, line, CAC, "cac:AccountingCustomerParty");
            Element customerId = basic(document, customer, "cbc:CustomerAssignedAccountID",
                    comprobante.getCliente().getNumeroDocumento());
            String customerType = documentCode(comprobante.getCliente().getTipoDocumento());
            customerId.setAttribute("schemeID", customerType);
            basic(document, customer, "cbc:AdditionalAccountID", customerType);
            Element party = child(document, customer, CAC, "cac:Party");
            Element legalEntity = child(document, party, CAC, "cac:PartyLegalEntity");
            basic(document, legalEntity, "cbc:RegistrationName", comprobante.getCliente().getNombreRazonSocial());
        }
        return serialize(document);
    }

    public byte[] baja(String id, Tenant tenant, LocalDate issueDate, List<Comprobante> documents, String reason) {
        Document document = create();
        Element root = root(document, VOIDED_NS, "VoidedDocuments");
        commonHeader(document, root, "RA-" + issueDate.format(DATE_ID) + "-" + id,
                issueDate, LocalDate.now(), tenant);
        int lineNumber = 1;
        for (Comprobante comprobante : documents) {
            Element line = child(document, root, CAC, "cac:VoidedDocumentsLine");
            basic(document, line, "cbc:LineID", Integer.toString(lineNumber++));
            basic(document, line, "cbc:DocumentTypeCode", comprobante.getTipoComprobante());
            basic(document, line, "cbc:DocumentSerialID", comprobante.getSerie());
            basic(document, line, "cbc:DocumentNumberID", String.valueOf(comprobante.getNumero()));
            basic(document, line, "cbc:VoidReasonDescription", reason);
        }
        return serialize(document);
    }

    private void commonHeader(Document document, Element root, String id, LocalDate referenceDate,
                              LocalDate issueDate, Tenant tenant) {
        Element extensions = child(document, root, EXT, "ext:UBLExtensions");
        child(document, child(document, extensions, EXT, "ext:UBLExtension"), EXT, "ext:ExtensionContent");
        basic(document, root, "cbc:UBLVersionID", "2.0");
        basic(document, root, "cbc:CustomizationID", "1.1");
        basic(document, root, "cbc:ID", id);
        basic(document, root, "cbc:ReferenceDate", referenceDate.toString());
        basic(document, root, "cbc:IssueDate", issueDate.toString());
        Element supplier = child(document, root, CAC, "cac:AccountingSupplierParty");
        Element party = child(document, supplier, CAC, "cac:Party");
        Element identification = child(document, party, CAC, "cac:PartyIdentification");
        Element idElement = basic(document, identification, "cbc:ID", tenant.getRuc());
        idElement.setAttribute("schemeID", "6");
        Element legal = child(document, party, CAC, "cac:PartyLegalEntity");
        basic(document, legal, "cbc:RegistrationName", tenant.getRazonSocial());
    }

    private void addTax(Document document, Element line, String schemeId, String name, String reasonCode,
                        java.math.BigDecimal base, java.math.BigDecimal tax, String currency) {
        Element subtotal = child(document, line, CAC, "cac:TaxTotal");
        Element taxAmount = basic(document, subtotal, "cbc:TaxAmount", tax.toPlainString());
        taxAmount.setAttribute("currencyID", currency);
        Element category = child(document, subtotal, CAC, "cac:TaxSubtotal");
        Element taxable = basic(document, category, "cbc:TaxableAmount", base.toPlainString());
        taxable.setAttribute("currencyID", currency);
        Element amount = basic(document, category, "cbc:TaxAmount", tax.toPlainString());
        amount.setAttribute("currencyID", currency);
        Element taxCategory = child(document, category, CAC, "cac:TaxCategory");
        String categoryCode = switch (schemeId) {
            case "1000" -> "S";
            case "9997" -> "E";
            case "9996" -> "G";
            default -> "O";
        };
        Element categoryId = basic(document, taxCategory, "cbc:ID", categoryCode);
        categoryId.setAttribute("schemeID", "UN/ECE 5305");
        Element reason = basic(document, taxCategory, "cbc:TaxExemptionReasonCode", reasonCode);
        reason.setAttribute("listAgencyName", "PE:SUNAT");
        Element scheme = child(document, taxCategory, CAC, "cac:TaxScheme");
        Element schemeIdElement = basic(document, scheme, "cbc:ID", schemeId);
        schemeIdElement.setAttribute("schemeID", "UN/ECE 5153");
        basic(document, scheme, "cbc:Name", name);
        basic(document, scheme, "cbc:TaxTypeCode", "1000".equals(schemeId) ? "VAT" : "FRE");
    }

    private String documentCode(String documentType) {
        return switch (documentType == null ? "" : documentType.toUpperCase()) {
            case "DNI" -> "1";
            case "RUC" -> "6";
            case "CE" -> "4";
            case "PASAPORTE" -> "7";
            default -> "0";
        };
    }

    private void amount(Document document, Element parent, java.math.BigDecimal amount, String currency) {
        Element value = basic(document, parent, "cbc:Amount", amount.toPlainString());
        value.setAttribute("currencyID", currency);
    }

    private Element root(Document document, String namespace, String name) {
        Element root = document.createElementNS(namespace, name);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC);
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT);
        document.appendChild(root);
        return root;
    }

    private Element child(Document document, Element parent, String namespace, String qualifiedName) {
        Element element = document.createElementNS(namespace, qualifiedName);
        parent.appendChild(element);
        return element;
    }

    private Element basic(Document document, Element parent, String name, String value) {
        Element element = document.createElementNS(CBC, name);
        element.setTextContent(value == null ? "" : value);
        parent.appendChild(element);
        return element;
    }

    private Document create() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return factory.newDocumentBuilder().newDocument();
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudo inicializar el XML UBL del lote SUNAT.", exception);
        }
    }

    private byte[] serialize(Document document) {
        try {
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
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudo serializar el XML UBL del lote.", exception);
        }
    }
}
