package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistStore;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsPersonalPlaylistCommandResponse(
    String service,
    String status,
    Instant processedAt,
    PmsPersonalPlaylistBootstrapResponse.Playlist playlist,
    String nextStepMessage
) {

    public static PmsPersonalPlaylistCommandResponse from(
        String status,
        PmsPersonalPlaylistStore.PersonalPlaylistState playlist,
        String nextStepMessage
    ) {
        return new PmsPersonalPlaylistCommandResponse(
            "pms-personal-playlists",
            status,
            Instant.now(),
            PmsPersonalPlaylistBootstrapResponse.Playlist.from(playlist),
            nextStepMessage
        );
    }
}
