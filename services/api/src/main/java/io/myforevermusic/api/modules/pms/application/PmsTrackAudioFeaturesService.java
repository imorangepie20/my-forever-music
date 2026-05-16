package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsUserTrackEntity;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsUserTrackRepository;
import io.myforevermusic.api.modules.pms.presentation.PmsTrackAudioFeaturesResponse;
import org.springframework.stereotype.Service;

/**
 * Lookup helper for the visualizer (see docs/architecture/PLAYBACK_VISUALIZER_DESIGN.md §4.1).
 * Returns ReccoBeats-filled audio features for a track by its `audio_feature_track_id`
 * (typically the Spotify track id). When no row exists, returns an `unresolved`
 * response — visualizer falls back to mode preset envelope.
 */
@Service
public class PmsTrackAudioFeaturesService {

    private final PmsUserTrackRepository userTrackRepository;

    public PmsTrackAudioFeaturesService(PmsUserTrackRepository userTrackRepository) {
        this.userTrackRepository = userTrackRepository;
    }

    public PmsTrackAudioFeaturesResponse getAudioFeatures(String userId, String audioFeatureTrackId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id is required for PMS audio features lookup.");
        }
        if (audioFeatureTrackId == null || audioFeatureTrackId.isBlank()) {
            throw new IllegalArgumentException("audio_feature_track_id is required.");
        }

        return userTrackRepository.findFirstByAudioFeatureTrackId(audioFeatureTrackId)
            .map(PmsUserTrackEntity::getAudioFeatures)
            .map(PmsTrackAudioFeaturesResponse::from)
            .orElseGet(() -> PmsTrackAudioFeaturesResponse.unresolved(audioFeatureTrackId));
    }
}
