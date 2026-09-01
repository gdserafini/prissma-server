package br.pucpr.prissma_server.workspaces;

import br.pucpr.prissma_server.auth.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolve o workspace ativo da request e injeta o {@link WorkspaceContext}
 * nos details do Authentication. Roda DEPOIS do JwtAuthenticationFilter.
 *
 * Ordem de resolução:
 *   1. sem principal -> no-op (rotas públicas)
 *   2. candidatos: header X-Workspace-Id -> claim workspaceId do token
 *   3. VALIDA ANTES DE ESCOLHER: vence o primeiro candidato que existe E ao
 *      qual o usuário tem acesso (header stale de outra aba não trava ninguém)
 *   4. nenhum candidato válido -> fallback server-side: primário -> primeira
 *      membership ativa (front antigo e token antigo continuam funcionando)
 *   5. havia candidato explícito, nenhum válido e sem fallback -> 404 genérico
 *      ("Workspace not found"), NUNCA 403 — anti-enumeração
 *   6. sem candidato e sem fallback (membro puro recém-criado) -> segue sem
 *      contexto: endpoints escopados negam via 404; GET/POST /workspaces funcionam
 */
@Component
public class WorkspaceContextFilter extends OncePerRequestFilter {

    public static final String WORKSPACE_HEADER = "X-Workspace-Id";

    private final WorkspaceService workspaceService;

    public WorkspaceContextFilter(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        List<Long> candidates = new ArrayList<>(2);
        String header = request.getHeader(WORKSPACE_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                candidates.add(Long.valueOf(header.trim()));
            } catch (NumberFormatException ignored) {
                // header malformado é tratado como candidato inválido
            }
        }
        Object claim = request.getAttribute(JwtAuthenticationFilter.WORKSPACE_CLAIM_ATTRIBUTE);
        if (claim instanceof Long claimId) {
            candidates.add(claimId);
        }
        boolean hadExplicitCandidate = !candidates.isEmpty()
                || (header != null && !header.isBlank());

        WorkspaceContext ctx = null;
        for (Long candidate : candidates) {
            Optional<WorkspaceContext> resolved = workspaceService.resolveContext(candidate, userId);
            if (resolved.isPresent()) {
                ctx = resolved.get();
                break;
            }
        }

        if (ctx == null) {
            ctx = workspaceService.resolveFallbackContext(userId).orElse(null);
        }

        if (ctx == null && hadExplicitCandidate) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Workspace not found");
            return;
        }

        if (ctx != null && auth instanceof AbstractAuthenticationToken token) {
            token.setDetails(ctx);
        }

        filterChain.doFilter(request, response);
    }
}
