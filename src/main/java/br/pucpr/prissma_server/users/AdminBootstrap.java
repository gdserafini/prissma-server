package br.pucpr.prissma_server.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email}") String email,
                          @Value("${app.admin.password}") String password,
                          @Value("${app.admin.name}") String name) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        var admin = new User(name, email, passwordEncoder.encode(password), Role.ADMIN);
        userRepository.save(admin);
    }
}
