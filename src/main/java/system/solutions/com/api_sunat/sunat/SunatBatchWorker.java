package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SunatBatchWorker {

    private static final Logger log = LoggerFactory.getLogger(SunatBatchWorker.class);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final SunatBatchPersistence persistence;
    private final SunatBatchXmlGenerator xmlGenerator;
    private final UblSchemaValidator schemaValidator;
    private final XmlDigitalSigner signer;
    private final SunatDocumentStorage storage;
    private final SunatSoapClient soapClient;
    private final SunatCdrProcessor cdrProcessor;

    public void enviar(UUID batchId) {
        if (!persistence.claim(batchId, "PENDIENTE_ENVIO", "PROCESANDO")) {
            return;
        }
        try {
            SunatBatchPersistence.BatchData data = persistence.load(batchId);
            var batch = data.batch();
            String type = "RESUMEN".equals(batch.getTipoLote()) ? "RC" : "RA";
            String serial = String.format("%05d", batch.getCorrelativo());
            String fileName = data.tenant().getRuc() + "-" + type + "-"
                    + batch.getFechaDocumentos().format(FILE_DATE) + "-" + serial;
            byte[] unsigned = "RESUMEN".equals(batch.getTipoLote())
                    ? xmlGenerator.resumen(serial, data.tenant(), batch.getFechaDocumentos(), data.documents())
                    : xmlGenerator.baja(serial, data.tenant(), batch.getFechaDocumentos(),
                            data.documents(), batch.getMensajeRespuesta());
            schemaValidator.validar(unsigned, batch.getTipoLote());
            XmlDigitalSigner.SignedXml signed = signer.firmar(unsigned, data.tenant());
            schemaValidator.validar(signed.content(), batch.getTipoLote());
            String xmlPath = storage.guardarBatchXml(data.tenant().getId(), fileName, signed.content());
            byte[] zip = storage.zip(fileName + ".xml", signed.content());
            storage.guardarBatchZip(data.tenant().getId(), fileName, zip);
            String ticket = soapClient.enviarResumen(data.tenant(), fileName + ".zip", zip);
            persistence.markTicket(batchId, fileName + ".zip", xmlPath, ticket);
        } catch (Exception exception) {
            persistence.fail(batchId, message(exception));
            log.error("Falló el envío del lote SUNAT {}: {}", batchId, exception.getMessage(), exception);
        }
    }

    public void consultar(UUID batchId) {
        if (!persistence.claim(batchId, "CONSULTA_PENDIENTE", "CONSULTANDO")) {
            return;
        }
        try {
            SunatBatchPersistence.BatchData data = persistence.load(batchId);
            var response = soapClient.consultarTicket(data.tenant(), data.batch().getTicketSunat());
            if ("98".equals(response.statusCode())) {
                persistence.retryTicket(batchId);
                return;
            }
            if (!"0".equals(response.statusCode())) {
                persistence.complete(batchId, response.statusCode(), response.statusMessage(), null, false);
                return;
            }
            if (response.applicationResponseBase64() == null || response.applicationResponseBase64().isBlank()) {
                persistence.retryTicket(batchId);
                log.warn("SUNAT reportó estado final sin contenido CDR para el lote {}.", batchId);
                return;
            }
            byte[] cdr = Base64.getDecoder().decode(
                    response.applicationResponseBase64().replaceAll("\\s", ""));
            SunatReceipt receipt = cdrProcessor.procesar(cdr);
            String cdrPath = storage.guardarBatchCdr(data.tenant().getId(),
                    data.batch().getNombreArchivo().replaceFirst("\\.zip$", ""), cdr);
            String message = receipt.description();
            if (!receipt.notes().isBlank()) message = message + " | Observaciones: " + receipt.notes();
            persistence.complete(batchId, receipt.responseCode(), message, cdrPath,
                    "0".equals(receipt.responseCode()));
        } catch (Exception exception) {
            persistence.retryTicket(batchId);
            log.error("Falló la consulta del ticket SUNAT del lote {}: {}", batchId, exception.getMessage(), exception);
        }
    }

    private String message(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
