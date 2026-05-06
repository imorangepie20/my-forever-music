package io.myforevermusic.api.modules.gms.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.gms.infrastructure.ai.AiRecommendationPreviewClient;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import io.myforevermusic.api.modules.platform.application.LastFmScrobbleStore;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class GmsRecommendationPreviewService {

    private final AiRecommendationPreviewClient aiRecommendationPreviewClient;
    private final AuthAccountStore authAccountStore;
    private final LastFmScrobbleStore lastFmScrobbleStore;
    private final PmsUserLibraryStore pmsUserLibraryStore;
    private final Optional<LastFmWebApiClient> lastFmWebApiClient;

    public GmsRecommendationPreviewService(
        AiRecommendationPreviewClient aiRecommendationPreviewClient,
        AuthAccountStore authAccountStore,
        LastFmScrobbleStore lastFmScrobbleStore,
        PmsUserLibraryStore pmsUserLibraryStore,
        Optional<LastFmWebApiClient> lastFmWebApiClient
    ) {
        this.aiRecommendationPreviewClient = aiRecommendationPreviewClient;
        this.authAccountStore = authAccountStore;
        this.lastFmScrobbleStore = lastFmScrobbleStore;
        this.pmsUserLibraryStore = pmsUserLibraryStore;
        this.lastFmWebApiClient = lastFmWebApiClient;
    }

    public GmsRecommendationPreviewResponse previewRecommendations(GmsRecommendationPreviewRequest request) {
        List<String> enrichmentWarnings = new ArrayList<>();
        GmsRecommendationPreviewRequest enrichedRequest = enrichWithLastFmArtists(request, enrichmentWarnings);
        GmsRecommendationPreviewResponse response = aiRecommendationPreviewClient.requestPreview(enrichedRequest);
        List<GmsRecommendationPreviewResponse.RecommendationItem> playableItems = projectPlayableItems(
            enrichedRequest,
            response.items()
        );
        if ((response.items() != null && !response.items().isEmpty()) && playableItems.isEmpty()) {
            throw new IllegalArgumentException(
                "GMS recommendations require imported PMS user library tracks. Import a real playlist before requesting recommendations."
            );
        }

        if (!playableItems.isEmpty()) {
            enrichmentWarnings.add(
                "GMS preview items were resolved against the PMS user library so they can be played inside the rebuild shell."
            );
        }

        if (enrichmentWarnings.isEmpty()) {
            return playableItems.isEmpty() ? response : new GmsRecommendationPreviewResponse(
                response.requestId(),
                response.generatedAt(),
                response.service(),
                response.status(),
                response.context(),
                response.inputSummary(),
                playableItems,
                response.warnings()
            );
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
            playableItems.isEmpty() ? response.items() : playableItems,
            List.copyOf(mergedWarnings)
        );
    }

    private List<GmsRecommendationPreviewResponse.RecommendationItem> projectPlayableItems(
        GmsRecommendationPreviewRequest request,
        List<GmsRecommendationPreviewResponse.RecommendationItem> aiItems
    ) {
        if (aiItems == null || aiItems.isEmpty() || request.userId() == null || request.userId().isBlank()) {
            return List.of();
        }

        List<LibraryCandidateTrack> candidates = resolveLibraryCandidates(request.userId(), request.playlistId());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<String> seedTrackIds = normalizeValues(request.seedTrackIds());
        List<String> seedArtists = normalizeValues(request.seedArtistNames());
        List<String> seedGenres = normalizeValues(request.seedGenres());
        Set<String> seenTrackIds = new HashSet<>();

        List<RankedLibraryCandidate> rankedCandidates = candidates.stream()
            .map(candidate -> new RankedLibraryCandidate(
                candidate,
                roundScore(computeAffinity(candidate, request, seedTrackIds, seedArtists, seedGenres))
            ))
            .sorted(Comparator.comparingDouble(RankedLibraryCandidate::affinityScore).reversed()
                .thenComparing((RankedLibraryCandidate ranked) -> ranked.candidate().seed()).reversed()
                .thenComparing((RankedLibraryCandidate ranked) -> requestedPlaylistMatch(request.playlistId(), ranked.candidate())).reversed()
                .thenComparing(ranked -> ranked.candidate().sortOrder())
                .thenComparing(ranked -> ranked.candidate().trackId()))
            .filter(ranked -> seenTrackIds.add(ranked.candidate().trackId()))
            .limit(aiItems.size())
            .toList();

        if (rankedCandidates.isEmpty()) {
            return List.of();
        }

        return IntStream.range(0, Math.min(aiItems.size(), rankedCandidates.size()))
            .mapToObj(index -> toRecommendationItem(aiItems.get(index), rankedCandidates.get(index)))
            .toList();
    }

    private List<LibraryCandidateTrack> resolveLibraryCandidates(String userId, String requestedPlaylistId) {
        List<PmsUserLibraryStore.LibraryPlaylistState> playlists = pmsUserLibraryStore.findPlaylists(userId);
        if (playlists.isEmpty()) {
            return List.of();
        }

        return playlists.stream()
            .flatMap(playlist -> playlist.tracks().stream()
                .map(track -> new LibraryCandidateTrack(
                    playlist.playlistId(),
                    playlist.title(),
                    playlist.sourcePlatform(),
                    playlist.coverImageUrl(),
                    playlist.platformExternalUrl(),
                    playlist.platformUri(),
                    track.trackId(),
                    track.title(),
                    track.artistName(),
                    track.primaryGenre(),
                    track.albumTitle(),
                    track.albumImageUrl(),
                    track.platformExternalUrl(),
                    track.platformUri(),
                    track.previewUrl(),
                    track.seed(),
                    track.sortOrder(),
                    track.audioFeatures()
                )))
            .sorted(Comparator
                .comparing((LibraryCandidateTrack candidate) -> requestedPlaylistMatch(requestedPlaylistId, candidate))
                .reversed()
                .thenComparing(LibraryCandidateTrack::seed)
                .reversed()
                .thenComparing(LibraryCandidateTrack::sortOrder))
            .toList();
    }

    private GmsRecommendationPreviewResponse.RecommendationItem toRecommendationItem(
        GmsRecommendationPreviewResponse.RecommendationItem aiItem,
        RankedLibraryCandidate rankedCandidate
    ) {
        LibraryCandidateTrack candidate = rankedCandidate.candidate();
        String mergedReason = aiItem.reason();
        if (mergedReason == null || mergedReason.isBlank()) {
            mergedReason = buildLibraryReason(candidate);
        } else {
            mergedReason = "%s %s".formatted(aiItem.reason(), buildLibraryReason(candidate));
        }

        return new GmsRecommendationPreviewResponse.RecommendationItem(
            aiItem.rank(),
            candidate.trackId(),
            candidate.title(),
            candidate.artistName(),
            candidate.sourcePlatform(),
            candidate.playlistId(),
            candidate.playlistTitle(),
            candidate.albumTitle(),
            candidate.albumImageUrl(),
            firstNonBlank(candidate.platformExternalUrl(), candidate.playlistExternalUrl()),
            firstNonBlank(candidate.platformUri(), candidate.audioFeatures() == null ? null : candidate.audioFeatures().getSpotifyUri()),
            candidate.previewUrl(),
            candidate.audioFeatures() == null ? null : candidate.audioFeatures().getAudioFeatureTrackId(),
            candidate.audioFeatures() == null ? null : candidate.audioFeatures().getDurationMs(),
            rankedCandidate.affinityScore(),
            aiItem.sourceSpace(),
            aiItem.energyLevel(),
            mergedReason
        );
    }

    private double computeAffinity(
        LibraryCandidateTrack candidate,
        GmsRecommendationPreviewRequest request,
        List<String> seedTrackIds,
        List<String> seedArtists,
        List<String> seedGenres
    ) {
        double score = 0.42d;
        if (requestedPlaylistMatch(request.playlistId(), candidate)) {
            score += 0.09d;
        }
        if (candidate.seed()) {
            score += 0.07d;
        }
        if (seedTrackIds.contains(normalizeValue(candidate.trackId()))) {
            score += 0.15d;
        }
        if (seedArtists.contains(normalizeValue(candidate.artistName()))) {
            score += 0.16d;
        }
        if (seedGenres.contains(normalizeValue(candidate.primaryGenre()))) {
            score += 0.12d;
        }

        score += 0.14d * energyAlignment(candidate.audioFeatures(), request.energyLevel());
        score += 0.09d * moodAlignment(candidate, request.mood());

        return Math.min(0.99d, score);
    }

    private double energyAlignment(PmsTrackAudioFeatures features, Integer requestedEnergyLevel) {
        if (requestedEnergyLevel == null || features == null || features.getEnergy() == null) {
            return 0.5d;
        }

        double trackEnergyLevel = 1.0d + (features.getEnergy() * 4.0d);
        double delta = Math.abs(trackEnergyLevel - requestedEnergyLevel);
        return Math.max(0.0d, 1.0d - (delta / 4.0d));
    }

    private double moodAlignment(LibraryCandidateTrack candidate, String mood) {
        if (mood == null || mood.isBlank()) {
            return 0.5d;
        }

        String genre = normalizeValue(candidate.primaryGenre());
        PmsTrackAudioFeatures features = candidate.audioFeatures();
        double energy = features == null || features.getEnergy() == null ? 0.5d : features.getEnergy();
        double valence = features == null || features.getValence() == null ? 0.5d : features.getValence();

        return switch (mood) {
            case "focus" -> genre.contains("ambient") || genre.contains("lo-fi") || genre.contains("downtempo")
                ? 0.95d
                : energy <= 0.65d ? 0.72d : 0.45d;
            case "calm" -> valence <= 0.6d && energy <= 0.55d ? 0.9d : 0.5d;
            case "upbeat" -> energy >= 0.65d || valence >= 0.6d ? 0.92d : 0.46d;
            case "melancholy" -> valence <= 0.45d ? 0.88d : 0.44d;
            case "discovery" -> 0.68d;
            default -> 0.5d;
        };
    }

    private String buildLibraryReason(LibraryCandidateTrack candidate) {
        List<String> details = new ArrayList<>();
        if (candidate.seed()) {
            details.add("It already behaves like a strong PMS anchor track.");
        }
        if (candidate.primaryGenre() != null && !candidate.primaryGenre().isBlank()) {
            details.add("It preserves the %s direction.".formatted(candidate.primaryGenre()));
        }
        if (candidate.playlistTitle() != null && !candidate.playlistTitle().isBlank()) {
            details.add("It was pulled from '%s' so the preview stays playable.".formatted(candidate.playlistTitle()));
        }
        if (details.isEmpty()) {
            details.add("It was resolved from the synced PMS user library so it stays playable.");
        }
        return String.join(" ", details);
    }

    private boolean requestedPlaylistMatch(String requestedPlaylistId, LibraryCandidateTrack candidate) {
        return requestedPlaylistId != null
            && !requestedPlaylistId.isBlank()
            && requestedPlaylistId.equals(candidate.playlistId());
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
            .map(this::normalizeValue)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private double roundScore(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null || fallback.isBlank() ? null : fallback;
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

    private record LibraryCandidateTrack(
        String playlistId,
        String playlistTitle,
        String sourcePlatform,
        String playlistCoverImageUrl,
        String playlistExternalUrl,
        String playlistUri,
        String trackId,
        String title,
        String artistName,
        String primaryGenre,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        boolean seed,
        int sortOrder,
        PmsTrackAudioFeatures audioFeatures
    ) {
    }

    private record RankedLibraryCandidate(
        LibraryCandidateTrack candidate,
        double affinityScore
    ) {
    }
}
