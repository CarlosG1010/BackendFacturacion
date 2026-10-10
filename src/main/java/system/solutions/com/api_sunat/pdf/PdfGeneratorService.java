package system.solutions.com.api_sunat.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.ComprobanteDetalle;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.sunat.SunatProperties;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter ISSUE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color INK = new Color(35, 45, 55);
    private static final Color MUTED = new Color(95, 105, 115);
    private static final Color LIGHT = new Color(238, 242, 246);

    private final QrCodeGenerator qrCodeGenerator;
    private final SpanishAmountWords amountWords;
    private final SunatProperties properties;

    public PdfGeneratorService(QrCodeGenerator qrCodeGenerator, SpanishAmountWords amountWords,
                               SunatProperties properties) {
        this.qrCodeGenerator = qrCodeGenerator;
        this.amountWords = amountWords;
        this.properties = properties;
    }

    public byte[] generar(Comprobante comprobante, Tenant tenant, PrintFormat format) {
        validar(comprobante, tenant, format);
        Rectangle pageSize = pageSize(format, comprobante);
        float margin = format == PrintFormat.A4 ? 36f : millimetersToPoints(format == PrintFormat.TICKET_80MM ? 4 : 3);
        Document document = new Document(pageSize, margin, margin, margin, margin);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(12_000);
            PdfWriter.getInstance(document, output);
            document.addTitle(documentTitle(comprobante));
            document.addAuthor(tenant.getRazonSocial());
            document.addCreator("API de Facturación Electrónica");
            document.open();

            if (format == PrintFormat.A4) {
                renderA4(document, comprobante, tenant);
            } else {
                renderTicket(document, comprobante, tenant, format);
            }
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("No se pudo generar la representación impresa del comprobante.", exception);
        }
    }

    private void validar(Comprobante comprobante, Tenant tenant, PrintFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("El formato debe ser A4, TICKET_80MM o TICKET_58MM.");
        }
        if (comprobante.getHashTributario() == null || comprobante.getHashTributario().isBlank()) {
            throw new IllegalStateException("El PDF con QR estará disponible cuando el comprobante tenga hash XML firmado.");
        }
        if (comprobante.getFechaEmision() == null || comprobante.getCliente() == null
                || comprobante.getDetalles() == null || comprobante.getDetalles().isEmpty()) {
            throw new IllegalStateException("El comprobante no tiene todos los datos necesarios para imprimir.");
        }
        if (tenant == null || tenant.getRuc() == null || tenant.getRazonSocial() == null) {
            throw new IllegalStateException("No se encontraron los datos fiscales de la empresa emisora.");
        }
    }

    private Rectangle pageSize(PrintFormat format, Comprobante comprobante) {
        if (format == PrintFormat.A4) {
            return PageSize.A4;
        }
        float widthMm = format == PrintFormat.TICKET_80MM ? 80f : 58f;
        float lineHeightMm = format == PrintFormat.TICKET_80MM ? 11f : 14f;
        float heightMm = Math.max(195f, 175f + comprobante.getDetalles().size() * lineHeightMm);
        return new Rectangle(millimetersToPoints(widthMm), millimetersToPoints(heightMm));
    }

    private void renderA4(Document document, Comprobante comprobante, Tenant tenant) throws Exception {
        addHeader(document, comprobante, tenant, true);
        addPartyDetails(document, comprobante, tenant);
        addItemsTable(document, comprobante, true);
        addTotals(document, comprobante, true);
        addLegalFooter(document, comprobante, tenant, PrintFormat.A4);
    }

    private void renderTicket(Document document, Comprobante comprobante, Tenant tenant, PrintFormat format) throws Exception {
        addLogo(document, tenant, format == PrintFormat.TICKET_80MM ? 44f : 34f);
        addCentered(document, tenant.getRazonSocial(), font(10, Font.BOLD, INK));
        addCentered(document, "RUC: " + tenant.getRuc(), font(8, Font.NORMAL, INK));
        addCentered(document, tenant.getDireccionFiscal(), font(7, Font.NORMAL, MUTED));
        if (tenant.getTelefono() != null && !tenant.getTelefono().isBlank()) {
            addCentered(document, "TEL: " + tenant.getTelefono(), font(7, Font.NORMAL, MUTED));
        }
        addRule(document);
        addCentered(document, documentType(comprobante), font(9, Font.BOLD, INK));
        addCentered(document, comprobante.getSerie() + "-" + comprobante.getNumero(), font(10, Font.BOLD, INK));
        addKeyValue(document, "Fecha", comprobante.getFechaEmision().format(ISSUE_DATE));
        addKeyValue(document, "Cliente", comprobante.getCliente().getNombreRazonSocial());
        addKeyValue(document, "Documento", customerDocument(comprobante.getCliente()));
        addRule(document);
        addTicketItems(document, comprobante, format);
        addRule(document);
        addKeyValue(document, "OP. GRAVADA", money(comprobante.getMontoGravado(), comprobante.getMoneda()));
        addKeyValue(document, "OP. EXONERADA", money(comprobante.getMontoExonerado(), comprobante.getMoneda()));
        addKeyValue(document, "OP. INAFECTA", money(comprobante.getMontoInafecto(), comprobante.getMoneda()));
        addKeyValue(document, "IGV", money(comprobante.getMontoIgv(), comprobante.getMoneda()));
        Paragraph total = new Paragraph("TOTAL " + money(comprobante.getMontoTotal(), comprobante.getMoneda()),
                font(format == PrintFormat.TICKET_58MM ? 9 : 11, Font.BOLD, INK));
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(4);
        total.setSpacingAfter(3);
        document.add(total);
        addLeft(document, amountWords.toCurrency(comprobante.getMontoTotal(), comprobante.getMoneda()), font(7, Font.NORMAL, INK));
        addQr(document, comprobante, tenant, format == PrintFormat.TICKET_80MM ? 100 : 82);
        addLegalFooter(document, comprobante, tenant, format);
    }

    private void addHeader(Document document, Comprobante comprobante, Tenant tenant, boolean withLogo) throws Exception {
        PdfPTable header = new PdfPTable(new float[]{1.1f, 2.6f, 1.2f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(14);
        PdfPCell logoCell = cell("", Element.ALIGN_CENTER, LIGHT, 5);
        if (withLogo) {
            Image logo = loadLogo(tenant, 100f, 55f);
            if (logo != null) {
                logoCell = new PdfPCell(logo, true);
                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setPadding(5);
            }
        }
        header.addCell(logoCell);

        PdfPCell issuer = new PdfPCell();
        issuer.setBorder(Rectangle.NO_BORDER);
        issuer.setPadding(5);
        issuer.addElement(new Paragraph(tenant.getRazonSocial(), font(13, Font.BOLD, INK)));
        issuer.addElement(new Paragraph("RUC: " + tenant.getRuc(), font(9, Font.NORMAL, INK)));
        issuer.addElement(new Paragraph(tenant.getDireccionFiscal(), font(8, Font.NORMAL, MUTED)));
        if (tenant.getTelefono() != null && !tenant.getTelefono().isBlank()) {
            issuer.addElement(new Paragraph(tenant.getTelefono(), font(8, Font.NORMAL, MUTED)));
        }
        header.addCell(issuer);

        PdfPCell type = new PdfPCell();
        type.setBorderColor(new Color(180, 190, 200));
        type.setPadding(8);
        type.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph docType = new Paragraph(documentType(comprobante), font(9, Font.BOLD, INK));
        docType.setAlignment(Element.ALIGN_CENTER);
        Paragraph number = new Paragraph(comprobante.getSerie() + "-" + comprobante.getNumero(), font(12, Font.BOLD, INK));
        number.setAlignment(Element.ALIGN_CENTER);
        type.addElement(docType);
        type.addElement(number);
        type.addElement(centeredPhrase("RUC " + tenant.getRuc(), font(8, Font.NORMAL, MUTED)));
        header.addCell(type);
        document.add(header);
    }

    private void addPartyDetails(Document document, Comprobante comprobante, Tenant tenant) throws DocumentException {
        PdfPTable details = new PdfPTable(new float[]{1f, 1f});
        details.setWidthPercentage(100);
        details.setSpacingAfter(12);
        addInfoCell(details, "EMISOR", tenant.getRazonSocial() + "\n" + tenant.getDireccionFiscal());
        addInfoCell(details, "CLIENTE", comprobante.getCliente().getNombreRazonSocial() + "\n"
                + customerDocument(comprobante.getCliente()) + "\n" + safe(comprobante.getCliente().getDireccion()));
        addInfoCell(details, "FECHA DE EMISIÓN", comprobante.getFechaEmision().format(ISSUE_DATE));
        addInfoCell(details, "MONEDA", comprobante.getMoneda());
        if (comprobante.getComprobanteOrigen() != null) {
            addInfoCell(details, "COMPROBANTE REFERENCIADO",
                    comprobante.getComprobanteOrigen().getSerie() + "-" + comprobante.getComprobanteOrigen().getNumero());
            addInfoCell(details, "MOTIVO", safe(comprobante.getMotivoNota()));
        }
        document.add(details);
    }

    private void addItemsTable(Document document, Comprobante comprobante, boolean a4) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.45f, 3.2f, 0.8f, 1.1f, 1.2f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(10);
        String[] headers = {"#", "DESCRIPCIÓN", "CANT.", "P. UNIT.", "TOTAL"};
        for (String header : headers) {
            table.addCell(cell(header, Element.ALIGN_CENTER, LIGHT, 6));
        }
        int index = 1;
        for (ComprobanteDetalle detail : comprobante.getDetalles()) {
            table.addCell(cell(String.valueOf(index++), Element.ALIGN_CENTER, Color.WHITE, 6));
            table.addCell(cell(detail.getProducto().getDescripcion(), Element.ALIGN_LEFT, Color.WHITE, 6));
            table.addCell(cell(detail.getCantidad().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT, Color.WHITE, 6));
            table.addCell(cell(money(detail.getPrecioUnitario(), comprobante.getMoneda()), Element.ALIGN_RIGHT, Color.WHITE, 6));
            table.addCell(cell(money(detail.getTotal(), comprobante.getMoneda()), Element.ALIGN_RIGHT, Color.WHITE, 6));
        }
        document.add(table);
    }

    private void addTicketItems(Document document, Comprobante comprobante, PrintFormat format) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{3f, 1.15f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        table.setSpacingAfter(2);
        table.addCell(cell("DESCRIPCIÓN / CANT. x P. UNIT.", Element.ALIGN_LEFT, LIGHT, 6));
        table.addCell(cell("TOTAL", Element.ALIGN_RIGHT, LIGHT, 6));
        for (ComprobanteDetalle detail : comprobante.getDetalles()) {
            table.addCell(cell(detail.getProducto().getDescripcion() + "\n"
                            + detail.getCantidad().stripTrailingZeros().toPlainString() + " x "
                            + money(detail.getPrecioUnitario(), comprobante.getMoneda()),
                    Element.ALIGN_LEFT, Color.WHITE, format == PrintFormat.TICKET_58MM ? 6 : 7));
            table.addCell(cell(money(detail.getTotal(), comprobante.getMoneda()), Element.ALIGN_RIGHT, Color.WHITE,
                    format == PrintFormat.TICKET_58MM ? 6 : 7));
        }
        document.add(table);
    }

    private void addTotals(Document document, Comprobante comprobante, boolean includeSubtotal) throws DocumentException {
        PdfPTable totals = new PdfPTable(new float[]{2.1f, 1f});
        totals.setWidthPercentage(42);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingBefore(4);
        if (includeSubtotal) {
            addTotalRow(totals, "OP. GRAVADA", money(comprobante.getMontoGravado(), comprobante.getMoneda()), false);
        }
        addTotalRow(totals, "OP. EXONERADA", money(comprobante.getMontoExonerado(), comprobante.getMoneda()), false);
        addTotalRow(totals, "OP. INAFECTA", money(comprobante.getMontoInafecto(), comprobante.getMoneda()), false);
        addTotalRow(totals, "OP. GRATUITA", money(comprobante.getMontoGratuito(), comprobante.getMoneda()), false);
        addTotalRow(totals, "IGV", money(comprobante.getMontoIgv(), comprobante.getMoneda()), false);
        addTotalRow(totals, "IMPORTE TOTAL", money(comprobante.getMontoTotal(), comprobante.getMoneda()), true);
        document.add(totals);
        Paragraph words = new Paragraph(amountWords.toCurrency(comprobante.getMontoTotal(), comprobante.getMoneda()), font(9, Font.BOLD, INK));
        words.setSpacingBefore(8);
        words.setSpacingAfter(7);
        document.add(words);
    }

    private void addLegalFooter(Document document, Comprobante comprobante, Tenant tenant, PrintFormat format) throws Exception {
        if (format == PrintFormat.A4) {
            addQr(document, comprobante, tenant, 120);
        }
        String kind = switch (comprobante.getTipoComprobante()) {
            case "01" -> "Factura Electrónica";
            case "03" -> "Boleta de Venta Electrónica";
            case "07" -> "Nota de Crédito Electrónica";
            case "08" -> "Nota de Débito Electrónica";
            default -> "Comprobante Electrónico";
        };
        addCentered(document, "Representación Impresa de la " + kind, font(8, Font.BOLD, INK));
        if ("ACEPTADO".equals(comprobante.getEstadoSunat())) {
            addCentered(document, "Estado de autorización SUNAT: ACEPTADO", font(7, Font.NORMAL, MUTED));
        } else if (comprobante.getEstadoSunat() != null) {
            addCentered(document, "Estado SUNAT: " + comprobante.getEstadoSunat(), font(7, Font.NORMAL, MUTED));
        }
        if (comprobante.getMensajeRespuestaSunat() != null && !comprobante.getMensajeRespuestaSunat().isBlank()) {
            addCentered(document, "Respuesta SUNAT: " + comprobante.getMensajeRespuestaSunat(), font(7, Font.NORMAL, MUTED));
        }
        addCentered(document, "Consulte la validez de este comprobante en los canales oficiales de SUNAT.",
                font(7, Font.NORMAL, MUTED));
    }

    private void addQr(Document document, Comprobante comprobante, Tenant tenant, int size) throws Exception {
        String payload = qrPayload(comprobante, tenant);
        BufferedImage qr = qrCodeGenerator.generate(payload, size);
        Image image = Image.getInstance(qr, null);
        image.scaleToFit(size, size);
        image.setAlignment(Image.ALIGN_CENTER);
        Paragraph container = new Paragraph();
        container.setAlignment(Element.ALIGN_CENTER);
        container.setSpacingBefore(4);
        container.add(new Chunk(image, 0, 0, true));
        document.add(container);
    }

    private String qrPayload(Comprobante comprobante, Tenant tenant) {
        Cliente cliente = comprobante.getCliente();
        return String.join("|",
                tenant.getRuc(),
                comprobante.getTipoComprobante(),
                comprobante.getSerie(),
                String.valueOf(comprobante.getNumero()),
                money(comprobante.getMontoIgv(), comprobante.getMoneda()),
                money(comprobante.getMontoTotal(), comprobante.getMoneda()),
                comprobante.getFechaEmision().toLocalDate().toString(),
                customerDocumentCode(cliente),
                safe(cliente.getNumeroDocumento()),
                comprobante.getHashTributario()
        );
    }

    private void addLogo(Document document, Tenant tenant, float maxWidth) throws Exception {
        Image logo = loadLogo(tenant, maxWidth, 48f);
        if (logo != null) {
            logo.setAlignment(Image.ALIGN_CENTER);
            document.add(logo);
        }
    }

    private Image loadLogo(Tenant tenant, float maxWidth, float maxHeight) throws Exception {
        if (tenant.getLogoPath() == null || tenant.getLogoPath().isBlank()) {
            return null;
        }
        Path root = properties.getLogoRoot().toRealPath();
        Path configuredPath = Path.of(tenant.getLogoPath());
        Path logoPath = (configuredPath.isAbsolute() ? configuredPath : root.resolve(configuredPath)).toRealPath();
        if (!logoPath.startsWith(root) || !Files.isRegularFile(logoPath)) {
            throw new IllegalStateException("La ruta del logo debe estar dentro del directorio de logos autorizado.");
        }
        BufferedImage bufferedImage = ImageIO.read(logoPath.toFile());
        if (bufferedImage == null) {
            throw new IllegalStateException("El logo configurado no es una imagen compatible.");
        }
        Image image = Image.getInstance(bufferedImage, null);
        image.scaleToFit(maxWidth, maxHeight);
        return image;
    }

    private void addKeyValue(Document document, String key, String value) throws DocumentException {
        PdfPTable row = new PdfPTable(new float[]{1f, 1.4f});
        row.setWidthPercentage(100);
        row.addCell(cell(key, Element.ALIGN_LEFT, Color.WHITE, 6));
        row.addCell(cell(value, Element.ALIGN_RIGHT, Color.WHITE, 6));
        document.add(row);
    }

    private void addInfoCell(PdfPTable table, String title, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(220, 225, 230));
        cell.setPadding(8);
        cell.addElement(new Paragraph(title, font(7, Font.BOLD, MUTED)));
        cell.addElement(new Paragraph(value, font(9, Font.NORMAL, INK)));
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean bold) {
        Font font = font(bold ? 10 : 9, bold ? Font.BOLD : Font.NORMAL, INK);
        table.addCell(cell(label, Element.ALIGN_RIGHT, bold ? LIGHT : Color.WHITE, 5, font));
        table.addCell(cell(value, Element.ALIGN_RIGHT, bold ? LIGHT : Color.WHITE, 5, font));
    }

    private PdfPCell cell(String text, int alignment, Color background, float padding) {
        return cell(text, alignment, background, padding, font(8, Font.NORMAL, INK));
    }

    private PdfPCell cell(String text, int alignment, Color background, float padding, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(background);
        cell.setBorderColor(new Color(220, 225, 230));
        cell.setPadding(padding);
        return cell;
    }

    private void addCentered(Document document, String text, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(2);
        document.add(paragraph);
    }

    private void addLeft(Document document, String text, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_LEFT);
        paragraph.setSpacingBefore(2);
        paragraph.setSpacingAfter(3);
        document.add(paragraph);
    }

    private Phrase centeredPhrase(String text, Font font) {
        Phrase phrase = new Phrase(text, font);
        return phrase;
    }

    private void addRule(Document document) throws DocumentException {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(3);
        rule.setSpacingAfter(3);
        PdfPCell line = new PdfPCell(new Phrase(""));
        line.setBorder(Rectangle.BOTTOM);
        line.setBorderColor(LIGHT);
        line.setFixedHeight(1.5f);
        rule.addCell(line);
        document.add(rule);
    }

    private String documentType(Comprobante comprobante) {
        return switch (comprobante.getTipoComprobante()) {
            case "01" -> "FACTURA ELECTRÓNICA";
            case "03" -> "BOLETA DE VENTA ELECTRÓNICA";
            case "07" -> "NOTA DE CRÉDITO ELECTRÓNICA";
            case "08" -> "NOTA DE DÉBITO ELECTRÓNICA";
            default -> "COMPROBANTE ELECTRÓNICO";
        };
    }

    private String customerDocument(Cliente cliente) {
        return safe(cliente.getTipoDocumento()) + ": " + safe(cliente.getNumeroDocumento());
    }

    private String customerDocumentCode(Cliente cliente) {
        return switch (cliente.getTipoDocumento() == null ? "" : cliente.getTipoDocumento().toUpperCase(Locale.ROOT)) {
            case "DNI" -> "1";
            case "RUC" -> "6";
            case "CE" -> "4";
            case "PASAPORTE" -> "7";
            default -> throw new IllegalStateException("Tipo de documento de cliente sin código QR SUNAT.");
        };
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String money(BigDecimal amount, String currency) {
        return currency + " " + money(amount);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String documentTitle(Comprobante comprobante) {
        return comprobante.getTipoComprobante() + "-" + comprobante.getSerie() + "-" + comprobante.getNumero();
    }

    private float millimetersToPoints(float mm) {
        return mm * 72f / 25.4f;
    }

    private Font font(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }
}
