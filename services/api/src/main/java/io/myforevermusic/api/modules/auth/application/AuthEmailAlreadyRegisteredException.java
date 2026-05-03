package io.myforevermusic.api.modules.auth.application;

public class AuthEmailAlreadyRegisteredException extends RuntimeException {

    public AuthEmailAlreadyRegisteredException(String email) {
        super("An account with this email already exists: %s".formatted(email));
    }
}
