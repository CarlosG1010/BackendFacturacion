package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SunatEmissionWorker {

    private static final Logger log = LoggerFactory.getLogger(SunatEmissionWorker.class);
    private final SunatEmissionPersistence persistence;
    private final UblXmlGenerator xmlGenerator;
    private final UblSchemaValidator schemaValidator;
    private final XmlDigitalSigner digitalSigner;
    private final SunatDocumentStorage storage;
    private final SunatSoapClient soapClient;
    private final SunatCdrProcessor cdrProcessor;

    public void enviar(UUID comprobanteId) {
        if (!persistence.claim(comprobanteId)) {
            return;
        }
        try {
            SunatEmissionPersistence.EmissionData data = persistence.load(comprobanteId);
            var comprobante = data.comprobante();
            var tenant = data.tenant();

            byte[] unsignedXml = xmlGenerator.generar(comprobante, tenant);
            schemaValidator.validar(unsignedXml, comprobante.getTipoComprobante());
            XmlDigitalSigner.SignedXml signed = digitalSigner.firmar(unsignedXml, tenant);
            schemaValidator.validar(signed.content(), comprobante.getTipoComprobante());
            SunatDocumentStorage.StoredFiles files = storage.guardar(comprobante, tenant, signed.content());
            persistence.markSending(comprobanteId, signed.digestValue(), files.xmlPath());

            String fileName = files.zipPath().substring(files.zipPath().lastIndexOf(java.io.File.separator) + 1);
            SunatSoapResponse response = soapClient.enviarComprobante(tenant, fileName, files.zipBytes());
            byte[] cdrZip = Base64.getDecoder().decode(response.applicationResponseBase64().replaceAll("\\s", ""));
            SunatReceipt receipt = cdrProcessor.procesar(cdrZip);
            String cdrPath = storage.guardarCdr(comprobante, tenant, cdrZip);
            persistence.complete(comprobanteId, receipt, cdrPath);
            if (!"0".equals(receipt.responseCode())) {
                log.warn("SUNAT rechazó comprobante {} del tenant {} con código {}: {}",
                        comprobanteId, tenant.getId(), receipt.responseCode(), receipt.description());
            }
        } catch (Exception exception) {
            String message = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            persistence.markFailure(comprobanteId, message);
            log.error("Falló el envío SUNAT del comprobante {}: {}", comprobanteId, message, exception);
        }
    }
}
