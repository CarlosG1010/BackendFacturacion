package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.entity.Marca;
import java.util.List;
import java.util.UUID;

public interface MarcaService {

    Marca guardar(Marca marca);
    Marca actualizar(UUID id, Marca marcaDetalles);
    void desactivar(UUID id);
    List<Marca> filtrarPorNombre(String nombre);

}
