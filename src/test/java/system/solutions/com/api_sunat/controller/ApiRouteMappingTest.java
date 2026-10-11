package system.solutions.com.api_sunat.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiRouteMappingTest {

    @Test
    void controllerMappingsDoNotDuplicateTheApplicationContextPath() {
        Map<Class<?>, String> expectedMappings = Map.of(
                UsuarioController.class, "/usuarios",
                ClienteController.class, "/clientes",
                CategoriaController.class, "/categorias",
                MarcaController.class, "/marcas",
                ProductoController.class, "/productos",
                AuditoriaController.class, "/auditorias",
                ComprobanteController.class, "/comprobantes",
                EmpresaController.class, "/empresa",
                SunatBatchController.class, "/sunat/lotes",
                SerieController.class, "/series"
        );

        expectedMappings.forEach((controller, expectedPath) -> {
            RequestMapping mapping = controller.getDeclaredAnnotation(RequestMapping.class);
            assertEquals(expectedPath, mapping.value()[0], controller.getSimpleName());
            assertArrayEquals(new String[]{expectedPath}, mapping.value(), controller.getSimpleName());
        });
    }
}
