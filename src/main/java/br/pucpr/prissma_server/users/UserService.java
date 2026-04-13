package br.pucpr.prissma_server.users;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final UserValidator validator;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository,
                       UserValidator validator,
                       PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user) {
        validator.validateEmail(user.getEmail());
        validator.validatePassword(user.getPassword());
//        if(user.getRole() == Role.ADMIN) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                    "Cannot create user with ADMIN role");
//        }
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repository.save(user);
    }

    public List<User> getUsers() {
        return repository.findAll();
    }

    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateUser(Long id, UserRequest request) {
        User user = getUserById(id);

        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.email() != null) {
            validator.validateEmail(request.email());
            user.setEmail(request.email());
        }
        if (request.password() != null) {
            validator.validatePassword(request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null && request.role() != Role.ADMIN) {
            user.setRole(request.role());
        }

        return repository.save(user);
    }

    public void deleteUser(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        repository.deleteById(id);
    }
}
