package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.entity.Cliente;
import java.util.List;
import java.util.UUID;

public interface ClienteService {

    Cliente registrarCliente(Cliente cliente);
    Cliente actualizarCliente(UUID id, Cliente clienteDetalles);
    Cliente cambiarEstado(UUID id, String nuevoEstado);
    List<Cliente> buscarClientes(String nombre, String estado);
    Cliente buscarPorDocumento(String tipoDocumento, String numeroDocumento);

}
