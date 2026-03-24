package br.pucpr.prissma_server.users;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final UserValidator validator;

    public UserService(UserRepository repository, UserValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public User createUser(User user) {
        validator.validateEmail(user.getEmail());
        validator.validatePassword(user.getPassword());
        if(user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot create user with ADMIN role");
        }
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        return repository.save(user);
    }

    public List<User> getUsers() {
        return repository.findAll();
    }

    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateUser(Long id, User updatedUser) {
        validator.validateEmail(updatedUser.getEmail());
        validator.validatePassword(updatedUser.getPassword());
        User user = getUserById(id);
        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        user.setPassword(updatedUser.getPassword());
        if (updatedUser.getRole() != null && updatedUser.getRole() != Role.ADMIN) {
            user.setRole(updatedUser.getRole());
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
