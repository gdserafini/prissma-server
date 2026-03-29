package br.pucpr.prissma_server.users;

public record UserRequest(String name, String email, String password, Role role) {

    public User toUser() {
        return new User(name, email, password, role);
    }
}
