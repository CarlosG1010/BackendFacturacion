package system.solutions.com.api_sunat.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import system.solutions.com.api_sunat.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantInterceptorTest {

    private final TenantInterceptor interceptor = new TenantInterceptor();

    @AfterEach
    void cleanContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestedTenantMustMatchAuthenticatedTenant() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticate(tenantId);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void authenticatedTenantIsUsedAndClearedAfterRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticate(tenantId);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(tenantId, TenantContext.getCurrentTenant());

        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void duplicateTenantHeadersAreRejected() throws Exception {
        authenticate(UUID.randomUUID());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", UUID.randomUUID().toString());
        request.addHeader("X-Tenant-ID", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(400, response.getStatus());
    }

    private void authenticate(UUID tenantId) {
        AuthenticatedUser user = new AuthenticatedUser(
                UUID.randomUUID(), tenantId, "user@example.pe", "encoded", "ADMIN_EMPRESA", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "n/a", List.of()));
    }
}
