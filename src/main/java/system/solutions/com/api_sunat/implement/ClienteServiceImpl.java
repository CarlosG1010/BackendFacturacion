package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.repository.ClienteRepository;
import system.solutions.com.api_sunat.service.ClienteService;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.util.RequestInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService{

    private final ClienteRepository clienteRepository;

    @Override
    @Transactional
    public Cliente registrarCliente(Cliente cliente) {
        if (cliente == null || TenantContext.getCurrentTenant() == null) {
            throw new IllegalArgumentException("Se requiere cliente y empresa autenticada.");
        }
        validarDatos(cliente);
        cliente.setId(UUID.randomUUID());
        cliente.setTenantId(TenantContext.getCurrentTenant());
        cliente.setEstado("ACTIVO");
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente actualizarCliente(UUID id, Cliente clienteDetalles) {
        if (clienteDetalles == null) {
            throw new IllegalArgumentException("Los datos del cliente son obligatorios.");
        }
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        validarDatos(clienteDetalles);
        cliente.setNombreRazonSocial(clienteDetalles.getNombreRazonSocial());
        cliente.setDireccion(clienteDetalles.getDireccion());
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente cambiarEstado(UUID id, String nuevoEstado) {
        validarEstado(nuevoEstado);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        cliente.setEstado(nuevoEstado);
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> buscarClientes(String nombre, String estado) {
        String query = RequestInput.filterText(nombre, "nombre", 100);
        validarEstado(estado);
        return clienteRepository.findByNombreRazonSocialContainingIgnoreCaseAndEstado(query, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorDocumento(String tipoDocumento, String numeroDocumento) {
        String type = RequestInput.requiredText(tipoDocumento, "tipoDocumento", 10).toUpperCase(java.util.Locale.ROOT);
        String number = RequestInput.requiredText(numeroDocumento, "numeroDocumento", 20).toUpperCase(java.util.Locale.ROOT);
        validarDocumento(type, number);
        return clienteRepository.findByTipoDocumentoAndNumeroDocumento(type, number)
                .orElseThrow(() -> new IllegalArgumentException("Cliente con el documento especificado no existe."));
    }

    private void validarDatos(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Los datos del cliente son obligatorios.");
        }
        String type = RequestInput.requiredText(cliente.getTipoDocumento(), "tipoDocumento", 10)
                .toUpperCase(java.util.Locale.ROOT);
        String number = RequestInput.requiredText(cliente.getNumeroDocumento(), "numeroDocumento", 20)
                .toUpperCase(java.util.Locale.ROOT);
        validarDocumento(type, number);
        cliente.setTipoDocumento(type);
        cliente.setNumeroDocumento(number);
        cliente.setNombreRazonSocial(RequestInput.requiredText(cliente.getNombreRazonSocial(), "nombreRazonSocial", 200));
        cliente.setDireccion(RequestInput.optionalText(cliente.getDireccion(), "direccion", 255));
    }

    private void validarDocumento(String type, String number) {
        boolean valid = switch (type) {
            case "DNI" -> number.matches("\\d{8}");
            case "RUC" -> number.matches("\\d{11}");
            case "CE", "PASAPORTE" -> number.matches("[A-Z0-9]{1,20}");
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Tipo o número de documento no válido.");
        }
    }

    private void validarEstado(String estado) {
        if (!"ACTIVO".equals(estado) && !"INACTIVO".equals(estado)) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }
    }
}
