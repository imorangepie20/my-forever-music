package io.myforevermusic.api.modules.auth.application;

public class AuthInvalidCredentialsException extends RuntimeException {

    public AuthInvalidCredentialsException() {
        super("Email or password is incorrect.");
    }
}
