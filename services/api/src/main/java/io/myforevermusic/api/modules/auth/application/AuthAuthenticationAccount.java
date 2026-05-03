package io.myforevermusic.api.modules.auth.application;

public record AuthAuthenticationAccount(
    AuthRegisteredAccount account,
    String passwordHash
) {
}
