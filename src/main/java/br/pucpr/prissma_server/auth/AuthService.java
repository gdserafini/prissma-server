package br.pucpr.prissma_server.auth;

import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserValidator;
import br.pucpr.prissma_server.workspaces.Workspace;
import br.pucpr.prissma_server.workspaces.WorkspaceContext;
import br.pucpr.prissma_server.workspaces.WorkspaceRole;
import br.pucpr.prissma_server.workspaces.WorkspaceService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkspaceService workspaceService;
    private final Algorithm algorithm;
    private final long expiration;
    private final String frontendUrl;

    public AuthService(
            AuthRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            UserValidator userValidator,
            ApplicationEventPublisher eventPublisher,
            WorkspaceService workspaceService,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expiration,
            @Value("${security.password-reset.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
        this.eventPublisher = eventPublisher;
        this.workspaceService = workspaceService;
        this.algorithm = Algorithm.HMAC256(secret);
        this.expiration = expiration;
        this.frontendUrl = frontendUrl;
    }

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Workspace do token: primário (criado no login se for a 1ª vez) ->
        // primeira membership ativa. Membro puro sem nada: claims omitidos —
        // o fallback server-side do WorkspaceContextFilter cobre as requests.
        Workspace primary = workspaceService.ensurePrimaryWorkspace(user.getId());
        WorkspaceContext ctx = primary != null
                ? new WorkspaceContext(primary.getId(), WorkspaceRole.OWNER, true)
                : workspaceService.resolveFallbackContext(user.getId()).orElse(null);

        return new LoginResponse(issueToken(user, ctx));
    }

    /** Emite o JWT com os claims de workspace. Reutilizado pelo switch de conta. */
    public String issueToken(User user, WorkspaceContext ctx) {
        var builder = JWT.create()
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration));
        if (ctx != null) {
            builder.withClaim("workspaceId", ctx.workspaceId())
                    .withClaim("workspaceRole", ctx.role().name())
                    .withClaim("isOwner", ctx.owner());
        }
        return builder.sign(algorithm);
    }

    @Transactional
    public void forgotPassword(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        resetTokenRepository.deleteByUser_Id(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        resetTokenRepository.save(new PasswordResetToken(token, user, expiresAt));

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        eventPublisher.publishEvent(new PasswordResetEmailEvent(user.getEmail(), resetLink));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "Invalid or expired token")
                );

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }

        userValidator.validatePassword(newPassword);

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetTokenRepository.delete(resetToken);
    }
}
