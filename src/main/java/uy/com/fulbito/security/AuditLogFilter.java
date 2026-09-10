package uy.com.fulbito.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class AuditLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditLogFilter.class);
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final JdbcTemplate jdbc;

    public AuditLogFilter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            if (MUTATING.contains(request.getMethod())) write(request, response);
        }
    }

    private void write(HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID actorId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try { actorId = UUID.fromString(authentication.getName()); }
                catch (IllegalArgumentException ignored) { /* solicitud publica */ }
            }
            String path = request.getRequestURI();
            if (path.length() > 300) path = path.substring(0, 300);
            jdbc.update(
                "insert into audit_events(actor_user_id,http_method,request_path,response_status) values (?,?,?,?)",
                actorId, request.getMethod(), path, response.getStatus()
            );
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar el evento de auditoria", ex);
        }
    }
}
