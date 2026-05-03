package io.myforevermusic.api.modules.platform.application;

import java.time.Instant;
import java.util.List;

public record PlatformTokenExchangeResult(
    String accessToken,
    String refreshToken,
    String tokenType,
    List<String> grantedScopes,
    Instant accessTokenExpiresAt
) {
}
