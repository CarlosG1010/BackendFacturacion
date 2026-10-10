package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.entity.Categoria;
import java.util.List;
import java.util.UUID;

public interface CategoriaService {

    Categoria guardar(Categoria categoria);
    Categoria actualizar(UUID id, Categoria categoriaDetalles);
    void desactivar(UUID id);
    List<Categoria> filtrarPorNombre(String nombre);

}
