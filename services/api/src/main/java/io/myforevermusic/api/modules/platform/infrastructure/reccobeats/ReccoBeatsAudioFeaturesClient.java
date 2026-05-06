package io.myforevermusic.api.modules.platform.infrastructure.reccobeats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReccoBeatsAudioFeaturesClient {

    private static final Logger log = LoggerFactory.getLogger(ReccoBeatsAudioFeaturesClient.class);
    private static final int BATCH_SIZE = 100;

    private final ReccoBeatsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ReccoBeatsAudioFeaturesClient(
        ReccoBeatsProperties properties,
        ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    ReccoBeatsAudioFeaturesClient(
        ReccoBeatsProperties properties,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public Map<String, ReccoBeatsAudioFeaturesSnapshot> getAudioFeaturesForSpotifyTrackIds(List<String> spotifyTrackIds) {
        if (!properties.isEnabled() || spotifyTrackIds == null || spotifyTrackIds.isEmpty()) {
            return Map.of();
        }

        Map<String, ReccoBeatsAudioFeaturesSnapshot> snapshots = new LinkedHashMap<>();
        List<String> normalizedTrackIds = spotifyTrackIds.stream()
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .toList();

        for (int index = 0; index < normalizedTrackIds.size(); index += BATCH_SIZE) {
            List<String> batch = normalizedTrackIds.subList(index, Math.min(index + BATCH_SIZE, normalizedTrackIds.size()));
            if (batch.isEmpty()) {
                continue;
            }

            AudioFeaturesEnvelope payload = get(buildAudioFeaturesUri(batch), AudioFeaturesEnvelope.class);
            List<AudioFeaturesItemResponse> items = Optional.ofNullable(payload.content()).orElse(List.of());
            Instant resolvedAt = Instant.now();
            items.stream()
                .filter(item -> item != null && item.href() != null && !item.href().isBlank())
                .map(item -> toSnapshot(item, resolvedAt))
                .filter(snapshot -> snapshot.spotifyTrackId() != null && !snapshot.spotifyTrackId().isBlank())
                .forEach(snapshot -> snapshots.put(snapshot.spotifyTrackId(), snapshot));
        }

        return snapshots;
    }

    public Map<String, ReccoBeatsAudioFeaturesSnapshot> getAudioFeaturesForExternalTracksByIsrc(
        List<ReccoBeatsTrackLookupRequest> trackRequests
    ) {
        if (!properties.isEnabled() || trackRequests == null || trackRequests.isEmpty()) {
            return Map.of();
        }

        List<ReccoBeatsTrackLookupRequest> normalizedRequests = trackRequests.stream()
            .filter(request -> request != null && hasText(request.externalTrackId()) && hasText(request.isrc()))
            .map(request -> new ReccoBeatsTrackLookupRequest(
                request.externalTrackId(),
                request.title(),
                request.artistName(),
                request.durationMs(),
                normalizeIsrc(request.isrc())
            ))
            .toList();

        if (normalizedRequests.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ReccoBeatsTrackCandidate>> candidatesByIsrc = getTrackCandidatesByIsrc(
            normalizedRequests.stream().map(ReccoBeatsTrackLookupRequest::isrc).distinct().toList()
        );
        Map<String, ReccoBeatsTrackCandidate> selectedCandidatesByExternalTrackId = new LinkedHashMap<>();
        for (ReccoBeatsTrackLookupRequest request : normalizedRequests) {
            ReccoBeatsTrackCandidate candidate = selectBestCandidate(
                request,
                candidatesByIsrc.getOrDefault(request.isrc(), List.of())
            );
            if (candidate != null) {
                selectedCandidatesByExternalTrackId.put(request.externalTrackId(), candidate);
            }
        }

        Instant resolvedAt = Instant.now();
        Map<String, ReccoBeatsAudioFeaturesSnapshot> snapshotCacheByTrackId = new LinkedHashMap<>();
        for (ReccoBeatsTrackCandidate candidate : selectedCandidatesByExternalTrackId.values().stream().distinct().toList()) {
            try {
                snapshotCacheByTrackId.put(
                    candidate.reccoBeatsTrackId(),
                    getAudioFeaturesForReccoBeatsTrack(candidate, resolvedAt)
                );
            } catch (RuntimeException exception) {
                log.warn(
                    "ReccoBeats track audio-features lookup failed for {} ({}): {}",
                    candidate.reccoBeatsTrackId(),
                    candidate.trackTitle(),
                    exception.getMessage()
                );
            }
        }

        Map<String, ReccoBeatsAudioFeaturesSnapshot> snapshots = new LinkedHashMap<>();
        for (Map.Entry<String, ReccoBeatsTrackCandidate> entry : selectedCandidatesByExternalTrackId.entrySet()) {
            ReccoBeatsAudioFeaturesSnapshot snapshot = snapshotCacheByTrackId.get(entry.getValue().reccoBeatsTrackId());
            if (snapshot != null) {
                snapshots.put(entry.getKey(), snapshot);
            }
        }

        return snapshots;
    }

    private <T> T get(String uri, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(
                    "ReccoBeats API request failed (%s): %s".formatted(response.statusCode(), response.body())
                );
            }

            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("ReccoBeats API response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ReccoBeats API request was interrupted.", exception);
        }
    }

    private String buildAudioFeaturesUri(List<String> spotifyTrackIds) {
        return buildBatchLookupUri("audio-features", spotifyTrackIds);
    }

    private String buildTrackLookupUri(List<String> ids) {
        return buildBatchLookupUri("track", ids);
    }

    private String buildTrackAudioFeaturesUri(String reccoBeatsTrackId) {
        return "%s/track/%s/audio-features".formatted(
            trimTrailingSlash(properties.getBaseUrl()),
            URLEncoder.encode(reccoBeatsTrackId, StandardCharsets.UTF_8)
        );
    }

    private String buildBatchLookupUri(String path, List<String> ids) {
        List<String> encodedIds = new ArrayList<>(ids.size());
        for (String id : ids) {
            encodedIds.add(URLEncoder.encode(id, StandardCharsets.UTF_8));
        }
        return "%s/%s?ids=%s".formatted(trimTrailingSlash(properties.getBaseUrl()), path, String.join(",", encodedIds));
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.reccobeats.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private Map<String, List<ReccoBeatsTrackCandidate>> getTrackCandidatesByIsrc(List<String> isrcs) {
        Map<String, List<ReccoBeatsTrackCandidate>> candidatesByIsrc = new LinkedHashMap<>();
        for (int index = 0; index < isrcs.size(); index += BATCH_SIZE) {
            List<String> batch = isrcs.subList(index, Math.min(index + BATCH_SIZE, isrcs.size()));
            if (batch.isEmpty()) {
                continue;
            }

            TrackEnvelope payload = get(buildTrackLookupUri(batch), TrackEnvelope.class);
            List<TrackItemResponse> items = Optional.ofNullable(payload.content()).orElse(List.of());
            for (TrackItemResponse item : items) {
                ReccoBeatsTrackCandidate candidate = toTrackCandidate(item);
                if (candidate == null || !hasText(candidate.isrc())) {
                    continue;
                }
                candidatesByIsrc.computeIfAbsent(candidate.isrc(), ignored -> new ArrayList<>()).add(candidate);
            }
        }
        return candidatesByIsrc;
    }

    private ReccoBeatsAudioFeaturesSnapshot getAudioFeaturesForReccoBeatsTrack(
        ReccoBeatsTrackCandidate candidate,
        Instant resolvedAt
    ) {
        AudioFeaturesItemResponse item = get(
            buildTrackAudioFeaturesUri(candidate.reccoBeatsTrackId()),
            AudioFeaturesItemResponse.class
        );
        String spotifyTrackHref = hasText(item.href()) ? item.href() : candidate.spotifyTrackHref();
        return new ReccoBeatsAudioFeaturesSnapshot(
            extractSpotifyTrackId(spotifyTrackHref),
            candidate.reccoBeatsTrackId(),
            spotifyTrackHref,
            candidate.isrc(),
            item.acousticness(),
            item.danceability(),
            item.energy(),
            item.instrumentalness(),
            item.musicalKey(),
            item.liveness(),
            item.loudness(),
            item.mode(),
            item.speechiness(),
            item.tempo(),
            item.valence(),
            resolvedAt
        );
    }

    private ReccoBeatsAudioFeaturesSnapshot toSnapshot(AudioFeaturesItemResponse item, Instant resolvedAt) {
        String spotifyTrackId = extractSpotifyTrackId(item.href());
        if (spotifyTrackId == null) {
            log.debug("Skipping ReccoBeats audio feature item without a parseable Spotify href: {}", item.href());
        }
        return new ReccoBeatsAudioFeaturesSnapshot(
            spotifyTrackId,
            item.id(),
            item.href(),
            item.isrc(),
            item.acousticness(),
            item.danceability(),
            item.energy(),
            item.instrumentalness(),
            item.musicalKey(),
            item.liveness(),
            item.loudness(),
            item.mode(),
            item.speechiness(),
            item.tempo(),
            item.valence(),
            resolvedAt
        );
    }

    private ReccoBeatsTrackCandidate toTrackCandidate(TrackItemResponse item) {
        if (item == null || !hasText(item.id())) {
            return null;
        }

        String normalizedIsrc = normalizeIsrc(item.isrc());
        return new ReccoBeatsTrackCandidate(
            item.id(),
            firstNonBlank(item.trackTitle(), item.title()),
            joinArtists(item.artists()),
            item.durationMs(),
            normalizedIsrc,
            item.href(),
            item.popularity()
        );
    }

    private ReccoBeatsTrackCandidate selectBestCandidate(
        ReccoBeatsTrackLookupRequest request,
        List<ReccoBeatsTrackCandidate> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        return candidates.stream()
            .max((left, right) -> Integer.compare(scoreCandidate(request, left), scoreCandidate(request, right)))
            .orElse(null);
    }

    private int scoreCandidate(ReccoBeatsTrackLookupRequest request, ReccoBeatsTrackCandidate candidate) {
        int score = 0;
        String normalizedRequestTitle = normalizeText(request.title());
        String normalizedCandidateTitle = normalizeText(candidate.trackTitle());
        String normalizedRequestArtist = normalizeText(request.artistName());
        String normalizedCandidateArtist = normalizeText(candidate.artistName());

        if (hasText(normalizedRequestTitle) && normalizedRequestTitle.equals(normalizedCandidateTitle)) {
            score += 120;
        } else if (
            hasText(normalizedRequestTitle)
                && hasText(normalizedCandidateTitle)
                && (
                    normalizedRequestTitle.contains(normalizedCandidateTitle)
                        || normalizedCandidateTitle.contains(normalizedRequestTitle)
                )
        ) {
            score += 70;
        }

        if (hasText(normalizedRequestArtist) && normalizedRequestArtist.equals(normalizedCandidateArtist)) {
            score += 80;
        } else if (
            hasText(normalizedRequestArtist)
                && hasText(normalizedCandidateArtist)
                && (
                    normalizedRequestArtist.contains(normalizedCandidateArtist)
                        || normalizedCandidateArtist.contains(normalizedRequestArtist)
                )
        ) {
            score += 45;
        }

        if (request.durationMs() != null && request.durationMs() > 0 && candidate.durationMs() != null && candidate.durationMs() > 0) {
            int difference = Math.abs(request.durationMs() - candidate.durationMs());
            if (difference <= 1_500) {
                score += 60;
            } else if (difference <= 5_000) {
                score += 35;
            } else if (difference <= 10_000) {
                score += 15;
            }
        }

        if (candidate.popularity() != null) {
            score += Math.max(0, Math.min(candidate.popularity(), 20));
        }

        if (hasText(candidate.spotifyTrackHref())) {
            score += 5;
        }

        return score;
    }

    private String extractSpotifyTrackId(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(href);
            String[] segments = uri.getPath().split("/");
            for (int index = 0; index < segments.length - 1; index++) {
                if ("track".equals(segments[index]) && !segments[index + 1].isBlank()) {
                    return segments[index + 1];
                }
            }
        } catch (IllegalArgumentException exception) {
            log.debug("Could not parse Spotify href from ReccoBeats response: {}", href);
        }

        return null;
    }

    private String joinArtists(List<Object> artists) {
        if (artists == null || artists.isEmpty()) {
            return null;
        }

        return artists.stream()
            .map(this::extractArtistName)
            .filter(this::hasText)
            .distinct()
            .reduce((left, right) -> left + ", " + right)
            .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private String extractArtistName(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Object name = firstNonBlank(
                mapValue.get("name"),
                mapValue.get("artistName"),
                mapValue.get("displayName")
            );
            return name == null ? null : name.toString();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeIsrc(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeText(String value) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "");
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value instanceof String stringValue && hasText(stringValue)) {
                return stringValue;
            }
            if (value != null) {
                String stringValue = value.toString();
                if (hasText(stringValue)) {
                    return stringValue;
                }
            }
        }
        return null;
    }

    public record ReccoBeatsTrackLookupRequest(
        String externalTrackId,
        String title,
        String artistName,
        Integer durationMs,
        String isrc
    ) {
    }

    public record ReccoBeatsAudioFeaturesSnapshot(
        String spotifyTrackId,
        String reccoBeatsTrackId,
        String spotifyTrackHref,
        String isrc,
        Double acousticness,
        Double danceability,
        Double energy,
        Double instrumentalness,
        Integer musicalKey,
        Double liveness,
        Double loudness,
        Integer mode,
        Double speechiness,
        Double tempo,
        Double valence,
        Instant resolvedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AudioFeaturesEnvelope(
        @JsonProperty("content") List<AudioFeaturesItemResponse> content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrackEnvelope(
        @JsonProperty("content") List<TrackItemResponse> content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrackItemResponse(
        @JsonProperty("id") String id,
        @JsonProperty("trackTitle") String trackTitle,
        @JsonProperty("title") String title,
        @JsonProperty("artists") List<Object> artists,
        @JsonProperty("durationMs") Integer durationMs,
        @JsonProperty("isrc") String isrc,
        @JsonProperty("href") String href,
        @JsonProperty("popularity") Integer popularity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AudioFeaturesItemResponse(
        @JsonProperty("id") String id,
        @JsonProperty("href") String href,
        @JsonProperty("isrc") String isrc,
        @JsonProperty("acousticness") Double acousticness,
        @JsonProperty("danceability") Double danceability,
        @JsonProperty("energy") Double energy,
        @JsonProperty("instrumentalness") Double instrumentalness,
        @JsonProperty("key") Integer musicalKey,
        @JsonProperty("liveness") Double liveness,
        @JsonProperty("loudness") Double loudness,
        @JsonProperty("mode") Integer mode,
        @JsonProperty("speechiness") Double speechiness,
        @JsonProperty("tempo") Double tempo,
        @JsonProperty("valence") Double valence
    ) {
    }

    private record ReccoBeatsTrackCandidate(
        String reccoBeatsTrackId,
        String trackTitle,
        String artistName,
        Integer durationMs,
        String isrc,
        String spotifyTrackHref,
        Integer popularity
    ) {
    }
}
