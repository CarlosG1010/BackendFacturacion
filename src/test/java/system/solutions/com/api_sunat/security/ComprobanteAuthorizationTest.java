package system.solutions.com.api_sunat.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import system.solutions.com.api_sunat.dto.ComprobanteRequestDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprobanteAuthorizationTest {

    private final ComprobanteAuthorization authorization = new ComprobanteAuthorization();

    @Test
    void vendedorPuedeEmitirFacturaYBoletaPeroNoNotas() {
        var vendedor = authentication("ROLE_VENDEDOR");

        assertTrue(authorization.puedeEmitir(vendedor, request("01")));
        assertTrue(authorization.puedeEmitir(vendedor, request("03")));
        assertFalse(authorization.puedeEmitir(vendedor, request("07")));
        assertFalse(authorization.puedeEmitir(vendedor, request("08")));
    }

    @Test
    void contadorPuedeEmitirNotasPeroNoFacturasNiBoletas() {
        var contador = authentication("ROLE_CONTADOR");

        assertFalse(authorization.puedeEmitir(contador, request("01")));
        assertFalse(authorization.puedeEmitir(contador, request("03")));
        assertTrue(authorization.puedeEmitir(contador, request("07")));
        assertTrue(authorization.puedeEmitir(contador, request("08")));
    }

    private UsernamePasswordAuthenticationToken authentication(String role) {
        return new UsernamePasswordAuthenticationToken(
                "usuario@empresa.pe",
                "password",
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private ComprobanteRequestDTO request(String tipo) {
        ComprobanteRequestDTO request = new ComprobanteRequestDTO();
        request.setTipoComprobante(tipo);
        return request;
    }
}
