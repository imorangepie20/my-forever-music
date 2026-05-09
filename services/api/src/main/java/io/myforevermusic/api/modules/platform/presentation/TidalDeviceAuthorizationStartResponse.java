package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record TidalDeviceAuthorizationStartResponse(
    String service,
    String status,
    @JsonProperty("generated_at") Instant generatedAt,
    @JsonProperty("user_id") String userId,
    Authorization authorization
) {

    public record Authorization(
        @JsonProperty("device_code") String deviceCode,
        @JsonProperty("user_code") String userCode,
        @JsonProperty("verification_uri") String verificationUri,
        @JsonProperty("verification_uri_complete") String verificationUriComplete,
        @JsonProperty("expires_at") Instant expiresAt,
        @JsonProperty("interval_seconds") int intervalSeconds,
        @JsonProperty("requested_scopes") List<String> requestedScopes
    ) {
    }
}
