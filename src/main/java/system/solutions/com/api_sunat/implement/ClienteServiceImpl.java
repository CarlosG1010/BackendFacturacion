package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.repository.ClienteRepository;
import system.solutions.com.api_sunat.service.ClienteService;
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
        cliente.setEstado("ACTIVO");
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente actualizarCliente(UUID id, Cliente clienteDetalles) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        cliente.setNombreRazonSocial(clienteDetalles.getNombreRazonSocial());
        cliente.setDireccion(clienteDetalles.getDireccion());
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente cambiarEstado(UUID id, String nuevoEstado) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        cliente.setEstado(nuevoEstado);
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> buscarClientes(String nombre, String estado) {
        return clienteRepository.findByNombreRazonSocialContainingIgnoreCaseAndEstado(nombre, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorDocumento(String tipoDocumento, String numeroDocumento) {
        return clienteRepository.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)
                .orElseThrow(() -> new IllegalArgumentException("Cliente con el documento especificado no existe."));
    }
}
