package io.myforevermusic.api.modules.gms.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.gms.infrastructure.ai.AiRecommendationPreviewClient;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import io.myforevermusic.api.modules.platform.application.LastFmScrobbleStore;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GmsRecommendationPreviewService {

    private final AiRecommendationPreviewClient aiRecommendationPreviewClient;
    private final AuthAccountStore authAccountStore;
    private final LastFmScrobbleStore lastFmScrobbleStore;
    private final Optional<LastFmWebApiClient> lastFmWebApiClient;

    public GmsRecommendationPreviewService(
        AiRecommendationPreviewClient aiRecommendationPreviewClient,
        AuthAccountStore authAccountStore,
        LastFmScrobbleStore lastFmScrobbleStore,
        Optional<LastFmWebApiClient> lastFmWebApiClient
    ) {
        this.aiRecommendationPreviewClient = aiRecommendationPreviewClient;
        this.authAccountStore = authAccountStore;
        this.lastFmScrobbleStore = lastFmScrobbleStore;
        this.lastFmWebApiClient = lastFmWebApiClient;
    }

    public GmsRecommendationPreviewResponse previewRecommendations(GmsRecommendationPreviewRequest request) {
        List<String> enrichmentWarnings = new ArrayList<>();
        GmsRecommendationPreviewRequest enrichedRequest = enrichWithLastFmArtists(request, enrichmentWarnings);
        GmsRecommendationPreviewResponse response = aiRecommendationPreviewClient.requestPreview(enrichedRequest);

        if (enrichmentWarnings.isEmpty()) {
            return response;
        }

        List<String> mergedWarnings = new ArrayList<>(response.warnings());
        mergedWarnings.addAll(enrichmentWarnings);

        return new GmsRecommendationPreviewResponse(
            response.requestId(),
            response.generatedAt(),
            response.service(),
            response.status(),
            response.context(),
            response.inputSummary(),
            response.items(),
            List.copyOf(mergedWarnings)
        );
    }

    private GmsRecommendationPreviewRequest enrichWithLastFmArtists(
        GmsRecommendationPreviewRequest request,
        List<String> enrichmentWarnings
    ) {
        if (request.userId() == null || request.userId().isBlank()) {
            return request;
        }

        Optional<AuthRegisteredAccount> account = authAccountStore.findByUserId(request.userId());
        if (account.isEmpty() || account.get().lastFmUsername() == null || account.get().lastFmUsername().isBlank()) {
            return request;
        }

        String username = account.get().lastFmUsername();
        List<String> storedArtists = resolveStoredLastFmArtists(request.userId(), 3);
        if (!storedArtists.isEmpty()) {
            return mergeArtistSeeds(
                request,
                storedArtists,
                "Stored Last.fm scrobble snapshot '%s' contributed recent artist recurrence to this GMS preview.".formatted(
                    username
                ),
                enrichmentWarnings
            );
        }

        if (lastFmWebApiClient.isEmpty()) {
            return request;
        }

        try {
            List<String> lastFmArtists = lastFmWebApiClient.get()
                .getTopArtists(username, "1month", 3)
                .stream()
                .map(LastFmWebApiClient.LastFmTopArtist::artistName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .toList();

            if (lastFmArtists.isEmpty()) {
                return request;
            }

            return mergeArtistSeeds(
                request,
                lastFmArtists,
                "Saved Last.fm profile '%s' contributed top artist affinity to this GMS preview.".formatted(username),
                enrichmentWarnings
            );
        } catch (IllegalArgumentException exception) {
            enrichmentWarnings.add(
                "Last.fm artist affinity could not be blended into this GMS preview: %s".formatted(exception.getMessage())
            );
            return request;
        }
    }

    private GmsRecommendationPreviewRequest mergeArtistSeeds(
        GmsRecommendationPreviewRequest request,
        List<String> additionalArtists,
        String note,
        List<String> enrichmentWarnings
    ) {
        LinkedHashSet<String> mergedArtistSeeds = new LinkedHashSet<>(request.seedArtistNames());
        mergedArtistSeeds.addAll(additionalArtists);

        if (mergedArtistSeeds.size() == request.seedArtistNames().size()) {
            return request;
        }

        enrichmentWarnings.add(note);

        return new GmsRecommendationPreviewRequest(
            request.requestId(),
            request.userId(),
            request.playlistId(),
            request.mode(),
            request.mood(),
            request.energyLevel(),
            request.familiarityBias(),
            request.limit(),
            request.seedTrackIds(),
            List.copyOf(mergedArtistSeeds),
            request.seedGenres(),
            request.includeExplanations()
        );
    }

    private List<String> resolveStoredLastFmArtists(String userId, int limit) {
        java.util.LinkedHashMap<String, Integer> countsByArtist = new java.util.LinkedHashMap<>();

        lastFmScrobbleStore.getSnapshot(userId, 20).recentScrobbles().stream()
            .map(LastFmScrobbleStore.StoredScrobble::artistName)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(name -> !name.isBlank())
            .forEach(name -> countsByArtist.merge(name, 1, Integer::sum));

        return countsByArtist.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
}
