package io.myforevermusic.api.modules.pms.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PmsTrackAudioFeaturesResponse(
    String audioFeatureTrackId,
    String audioFeatureSource,
    Boolean audioFeaturesFilled,
    Integer durationMs,
    Integer musicalKey,
    Integer mode,
    Integer timeSignature,
    Double acousticness,
    Double danceability,
    Double energy,
    Double instrumentalness,
    Double liveness,
    Double loudness,
    Double speechiness,
    Double tempo,
    Double valence,
    Instant resolvedAt
) {

    public static PmsTrackAudioFeaturesResponse unresolved(String audioFeatureTrackId) {
        return new PmsTrackAudioFeaturesResponse(
            audioFeatureTrackId,
            "unresolved",
            false,
            null, null, null, null,
            null, null, null, null,
            null, null, null,
            null, null,
            null
        );
    }

    public static PmsTrackAudioFeaturesResponse from(PmsTrackAudioFeatures features) {
        return new PmsTrackAudioFeaturesResponse(
            features.getAudioFeatureTrackId(),
            features.getAudioFeatureSource(),
            features.isAudioFeaturesFilled(),
            features.getDurationMs(),
            features.getMusicalKey(),
            features.getMode(),
            features.getTimeSignature(),
            features.getAcousticness(),
            features.getDanceability(),
            features.getEnergy(),
            features.getInstrumentalness(),
            features.getLiveness(),
            features.getLoudness(),
            features.getSpeechiness(),
            features.getTempo(),
            features.getValence(),
            features.getResolvedAt()
        );
    }
}
