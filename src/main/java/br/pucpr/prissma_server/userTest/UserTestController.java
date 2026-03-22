package br.pucpr.prissma_server.userTest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserTestController {

    private final UserTestRepository repository;

    public UserTestController(UserTestRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/get-users-test")
    public List<UserTest> getUsersTest() {
        return repository.findAll();
    }
}
