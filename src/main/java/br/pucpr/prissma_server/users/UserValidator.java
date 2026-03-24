package br.pucpr.prissma_server.users;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Component
public class UserValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\..+$");
    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    public void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email formatW");
        }
    }

    public void validatePassword(String password) {
        if (password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (!PASSWORD_UPPERCASE.matcher(password).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least one uppercase letter");
        }
        if (!PASSWORD_LOWERCASE.matcher(password).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least one lowercase letter");
        }
        if (!PASSWORD_DIGIT.matcher(password).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least one digit");
        }
        if (!PASSWORD_SPECIAL.matcher(password).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least one special character");
        }
    }
}
