package io.myforevermusic.api.modules.platform.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TidalDeviceAuthorizationPollResponse(
    String service,
    String status,
    @JsonProperty("processed_at") Instant processedAt,
    @JsonProperty("user_id") String userId,
    @JsonProperty("requested_scopes") List<String> requestedScopes,
    Connection connection,
    String message
) {

    public record Connection(
        @JsonProperty("user_id") String userId,
        @JsonProperty("platform_id") String platformId,
        boolean connected,
        @JsonProperty("connection_status") String connectionStatus,
        @JsonProperty("connection_mode") String connectionMode,
        @JsonProperty("external_account_label") String externalAccountLabel,
        @JsonProperty("scope_summary") String scopeSummary,
        @JsonProperty("sync_ready") boolean syncReady,
        @JsonProperty("connected_at") Instant connectedAt
    ) {
    }
}
