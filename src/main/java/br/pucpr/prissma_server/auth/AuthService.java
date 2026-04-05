package br.pucpr.prissma_server.auth;

import br.pucpr.prissma_server.users.User;
import br.pucpr.prissma_server.users.UserValidator;
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
    private final Algorithm algorithm;
    private final long expiration;
    private final String frontendUrl;

    public AuthService(
            AuthRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            UserValidator userValidator,
            ApplicationEventPublisher eventPublisher,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expiration,
            @Value("${security.password-reset.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
        this.eventPublisher = eventPublisher;
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

        String token = JWT.create()
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(algorithm);

        return new LoginResponse(token);
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
