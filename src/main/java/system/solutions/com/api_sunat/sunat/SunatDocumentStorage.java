package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.Tenant;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
public class SunatDocumentStorage {

    private final SunatProperties properties;

    public StoredFiles guardar(Comprobante comprobante, Tenant tenant, byte[] signedXml) {
        try {
            Path root = properties.getDocumentRoot().toAbsolutePath().normalize();
            Path tenantDirectory = root.resolve(tenant.getId().toString()).normalize();
            if (!tenantDirectory.startsWith(root)) {
                throw new SunatIntegrationException("Ruta inválida para almacenamiento de documentos.");
            }
            Files.createDirectories(tenantDirectory);
            String fileName = tenant.getRuc() + "-" + comprobante.getTipoComprobante() + "-"
                    + comprobante.getSerie() + "-" + comprobante.getNumero();
            Path xmlPath = tenantDirectory.resolve(fileName + ".xml").normalize();
            Path zipPath = tenantDirectory.resolve(fileName + ".zip").normalize();
            if (!xmlPath.startsWith(tenantDirectory) || !zipPath.startsWith(tenantDirectory)) {
                throw new SunatIntegrationException("El nombre de comprobante no es válido para almacenamiento.");
            }
            Files.write(xmlPath, signedXml);
            ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
                zip.putNextEntry(new ZipEntry(fileName + ".xml"));
                zip.write(signedXml);
                zip.closeEntry();
            }
            byte[] zippedContent = zipBytes.toByteArray();
            Files.write(zipPath, zippedContent);
            return new StoredFiles(xmlPath.toString(), zipPath.toString(), zippedContent);
        } catch (SunatIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudieron guardar el XML firmado y el ZIP de envío.", exception);
        }
    }

    public String guardarCdr(Comprobante comprobante, Tenant tenant, byte[] cdrZip) {
        try {
            Path root = properties.getDocumentRoot().toAbsolutePath().normalize();
            Path tenantDirectory = root.resolve(tenant.getId().toString()).normalize();
            Files.createDirectories(tenantDirectory);
            String fileName = tenant.getRuc() + "-" + comprobante.getTipoComprobante() + "-"
                    + comprobante.getSerie() + "-" + comprobante.getNumero() + "-CDR.zip";
            Path cdrPath = tenantDirectory.resolve(fileName).normalize();
            if (!cdrPath.startsWith(tenantDirectory)) {
                throw new SunatIntegrationException("La ruta del CDR no es válida.");
            }
            Files.write(cdrPath, cdrZip);
            return cdrPath.toString();
        } catch (SunatIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SunatIntegrationException("No se pudo guardar el CDR recibido de SUNAT.", exception);
        }
    }

    public Resource cargarCdr(Comprobante comprobante) {
        if (comprobante.getCdrPath() == null || comprobante.getTenantId() == null) {
            throw new SunatIntegrationException("El CDR todavía no está disponible.");
        }
        Path root = properties.getDocumentRoot().toAbsolutePath().normalize();
        Path tenantDirectory = root.resolve(comprobante.getTenantId().toString()).normalize();
        Path cdrPath = Path.of(comprobante.getCdrPath()).toAbsolutePath().normalize();
        if (!cdrPath.startsWith(tenantDirectory) || !Files.isRegularFile(cdrPath)) {
            throw new SunatIntegrationException("No se encontró el archivo CDR del comprobante.");
        }
        return new FileSystemResource(cdrPath);
    }

    public record StoredFiles(String xmlPath, String zipPath, byte[] zipBytes) {
    }
}
