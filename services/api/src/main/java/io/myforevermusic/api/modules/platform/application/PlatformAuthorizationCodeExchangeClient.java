package io.myforevermusic.api.modules.platform.application;

public interface PlatformAuthorizationCodeExchangeClient {

    boolean supports(PlatformAuthorizationSession session);

    PlatformTokenExchangeResult exchangeAuthorizationCode(
        PlatformAuthorizationSession session,
        String authorizationCode
    );
}
