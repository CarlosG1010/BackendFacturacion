package system.solutions.com.api_sunat.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import system.solutions.com.api_sunat.security.AuthenticatedUser;

import java.util.UUID;
import java.util.Collections;
import java.util.Enumeration;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        TenantContext.clear();
        Enumeration<String> tenantHeaders = request.getHeaders(TENANT_HEADER);
        if (tenantHeaders != null && Collections.list(tenantHeaders).size() > 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Solo se permite un encabezado X-Tenant-ID.");
            return false;
        }
        String tenantHeader = request.getHeader(TENANT_HEADER);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return true;
        }

        UUID authenticatedTenantId = user.tenantId();
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            UUID requestedTenantId;
            try {
                if (!tenantHeader.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                    throw new IllegalArgumentException("UUID no canónico.");
                }
                requestedTenantId = UUID.fromString(tenantHeader);
            } catch (IllegalArgumentException exception) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "El formato de X-Tenant-ID no es un UUID válido.");
                return false;
            }

            if (!requestedTenantId.equals(authenticatedTenantId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "El tenant solicitado no corresponde al usuario autenticado.");
                return false;
            }
        }

        if (authenticatedTenantId != null) {
            TenantContext.setCurrentTenant(authenticatedTenantId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TenantContext.clear();
    }
}
