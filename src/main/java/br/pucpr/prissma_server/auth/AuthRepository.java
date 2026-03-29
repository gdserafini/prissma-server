package br.pucpr.prissma_server.auth;

import br.pucpr.prissma_server.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
