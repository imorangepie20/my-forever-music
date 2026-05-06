package io.myforevermusic.api.modules.gms.application;

import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationFeedbackRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationFeedbackResponse;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class GmsRecommendationFeedbackService {

    private static final Set<String> SUPPORTED_FEEDBACK_TYPES = Set.of("like", "dislike", "save", "skip");

    private final GmsRecommendationFeedbackStore feedbackStore;
    private final PmsUserLibraryStore pmsUserLibraryStore;

    public GmsRecommendationFeedbackService(
        GmsRecommendationFeedbackStore feedbackStore,
        PmsUserLibraryStore pmsUserLibraryStore
    ) {
        this.feedbackStore = feedbackStore;
        this.pmsUserLibraryStore = pmsUserLibraryStore;
    }

    public GmsRecommendationFeedbackResponse recordFeedback(GmsRecommendationFeedbackRequest request) {
        String feedbackType = normalizeFeedbackType(request.feedbackType());
        validateTrackBelongsToUserLibrary(request.userId(), request.trackId());

        GmsRecommendationFeedbackStore.StoredFeedback storedFeedback = feedbackStore.save(
            new GmsRecommendationFeedbackStore.FeedbackDraft(
                request.userId(),
                request.requestId(),
                request.playlistId(),
                request.trackId(),
                feedbackType,
                request.score(),
                request.sourceSpace(),
                request.reason()
            )
        );

        return GmsRecommendationFeedbackResponse.from(storedFeedback);
    }

    private String normalizeFeedbackType(String feedbackType) {
        String normalized = feedbackType == null ? "" : feedbackType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FEEDBACK_TYPES.contains(normalized)) {
            throw new IllegalArgumentException(
                "GMS feedback type must be one of: %s.".formatted(String.join(", ", SUPPORTED_FEEDBACK_TYPES))
            );
        }
        return normalized;
    }

    private void validateTrackBelongsToUserLibrary(String userId, String trackId) {
        List<PmsUserLibraryStore.LibraryPlaylistState> playlists = pmsUserLibraryStore.findPlaylists(userId);
        boolean exists = playlists.stream()
            .flatMap(playlist -> playlist.tracks().stream())
            .anyMatch(track -> track.trackId().equals(trackId));

        if (!exists) {
            throw new IllegalArgumentException(
                "GMS feedback can only be recorded for tracks in the user's synced PMS library."
            );
        }
    }
}
