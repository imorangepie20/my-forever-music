package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SpotifyWebApiClient {

    private final PlatformOAuthProperties platformOAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SpotifyWebApiClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper
    ) {
        this(platformOAuthProperties, objectMapper, HttpClient.newHttpClient());
    }

    SpotifyWebApiClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.platformOAuthProperties = platformOAuthProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public SpotifyUserProfile getCurrentUserProfile(PlatformAccountCredential credential) {
        SpotifyCurrentUserProfileResponse payload = get(
            credential,
            buildApiUri("/me"),
            SpotifyCurrentUserProfileResponse.class
        );

        return new SpotifyUserProfile(
            payload.id(),
            payload.displayName() == null || payload.displayName().isBlank() ? "Spotify User" : payload.displayName(),
            payload.email()
        );
    }

    public List<SpotifyPlaylistSummary> getCurrentUserPlaylists(PlatformAccountCredential credential) {
        List<SpotifyPlaylistSummary> playlists = new ArrayList<>();
        String nextUri = buildApiUri("/me/playlists?limit=50");

        while (nextUri != null && !nextUri.isBlank()) {
            SpotifyPlaylistPageResponse payload = get(credential, nextUri, SpotifyPlaylistPageResponse.class);
            List<SpotifyPlaylistItemResponse> items = Optional.ofNullable(payload.items()).orElse(List.of());
            items.stream()
                .filter(item -> item.id() != null && !item.id().isBlank())
                .map(item -> new SpotifyPlaylistSummary(
                    item.id(),
                    item.name() == null || item.name().isBlank() ? "Untitled Spotify Playlist" : item.name(),
                    item.description(),
                    item.owner() == null ? null : item.owner().id(),
                    item.owner() == null || item.owner().displayName() == null || item.owner().displayName().isBlank()
                        ? "Spotify"
                        : item.owner().displayName(),
                    item.collaborative() != null && item.collaborative(),
                    item.tracks() == null || item.tracks().total() == null ? 0 : item.tracks().total()
                ))
                .forEach(playlists::add);
            nextUri = payload.next();
        }

        return playlists;
    }

    public List<SpotifyPlaylistTrack> getPlaylistTracks(
        PlatformAccountCredential credential,
        String externalPlaylistId
    ) {
        List<SpotifyPlaylistTrack> tracks = new ArrayList<>();
        String nextUri = buildApiUri(
            "/playlists/%s/items?limit=100&additional_types=track".formatted(externalPlaylistId)
        );

        while (nextUri != null && !nextUri.isBlank()) {
            SpotifyPlaylistTrackPageResponse payload = get(credential, nextUri, SpotifyPlaylistTrackPageResponse.class);
            List<SpotifyPlaylistTrackItemResponse> items = Optional.ofNullable(payload.items()).orElse(List.of());
            items.stream()
                .map(SpotifyPlaylistTrackItemResponse::track)
                .filter(track -> track != null && track.id() != null && !track.id().isBlank())
                .filter(track -> track.isLocal() == null || !track.isLocal())
                .map(track -> new SpotifyPlaylistTrack(
                    track.id(),
                    track.name() == null || track.name().isBlank() ? "Untitled Spotify Track" : track.name(),
                    firstArtistName(track.artists()),
                    track.href(),
                    track.uri(),
                    track.durationMs()
                ))
                .forEach(tracks::add);
            nextUri = payload.next();
        }

        return tracks;
    }

    public Map<String, SpotifyAudioFeaturesSnapshot> getTrackAudioFeatures(
        PlatformAccountCredential credential,
        List<String> spotifyTrackIds
    ) {
        if (spotifyTrackIds == null || spotifyTrackIds.isEmpty()) {
            return Map.of();
        }

        Map<String, SpotifyAudioFeaturesSnapshot> snapshots = new LinkedHashMap<>();
        for (int index = 0; index < spotifyTrackIds.size(); index += 100) {
            List<String> batch = spotifyTrackIds.subList(index, Math.min(index + 100, spotifyTrackIds.size()));
            String joinedIds = batch.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(","));
            if (joinedIds.isBlank()) {
                continue;
            }

            SpotifyAudioFeaturesBatchResponse payload = get(
                credential,
                buildApiUri("/audio-features?ids=%s".formatted(joinedIds)),
                SpotifyAudioFeaturesBatchResponse.class
            );
            List<SpotifyAudioFeaturesItemResponse> audioFeatures = Optional.ofNullable(payload.audioFeatures()).orElse(List.of());
            audioFeatures.stream()
                .filter(item -> item != null && item.id() != null && !item.id().isBlank())
                .map(item -> new SpotifyAudioFeaturesSnapshot(
                    item.id(),
                    item.analysisUrl(),
                    item.trackHref(),
                    item.uri(),
                    item.featureType(),
                    item.durationMs(),
                    item.musicalKey(),
                    item.mode(),
                    item.timeSignature(),
                    item.acousticness(),
                    item.danceability(),
                    item.energy(),
                    item.instrumentalness(),
                    item.liveness(),
                    item.loudness(),
                    item.speechiness(),
                    item.tempo(),
                    item.valence(),
                    Instant.now()
                ))
                .forEach(snapshot -> snapshots.put(snapshot.spotifyTrackId(), snapshot));
        }

        return snapshots;
    }

    private <T> T get(
        PlatformAccountCredential credential,
        String uri,
        Class<T> responseType
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "%s %s".formatted(
                    credential.tokenType() == null || credential.tokenType().isBlank() ? "Bearer" : credential.tokenType(),
                    credential.accessToken()
                ))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(readErrorMessage(response.statusCode(), response.body()));
            }

            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Spotify API response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Spotify API request was interrupted.", exception);
        }
    }

    private String buildApiUri(String pathAndQuery) {
        String baseUri = platformOAuthProperties.getSpotify().getApiBaseUri();
        if (pathAndQuery.startsWith("http://") || pathAndQuery.startsWith("https://")) {
            return pathAndQuery;
        }
        return "%s%s".formatted(
            baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri,
            pathAndQuery.startsWith("/") ? pathAndQuery : "/%s".formatted(pathAndQuery)
        );
    }

    private String firstArtistName(List<SpotifyArtistResponse> artists) {
        return Optional.ofNullable(artists)
            .orElse(List.of())
            .stream()
            .map(SpotifyArtistResponse::name)
            .filter(name -> name != null && !name.isBlank())
            .findFirst()
            .orElse("Unknown Artist");
    }

    private String readErrorMessage(int statusCode, String body) {
        if (statusCode == 401) {
            return "Spotify access token is invalid or expired. Reconnect Spotify and try again.";
        }

        try {
            SpotifyApiErrorEnvelope errorEnvelope = objectMapper.readValue(body, SpotifyApiErrorEnvelope.class);
            if (errorEnvelope.error() != null && errorEnvelope.error().message() != null && !errorEnvelope.error().message().isBlank()) {
                return "Spotify API request failed (%s): %s".formatted(statusCode, errorEnvelope.error().message());
            }
        } catch (IOException ignored) {
            // Fall through to generic response below.
        }

        return body == null || body.isBlank()
            ? "Spotify API request failed with status %s.".formatted(statusCode)
            : "Spotify API request failed (%s): %s".formatted(statusCode, body);
    }

    public record SpotifyUserProfile(
        String spotifyUserId,
        String displayName,
        String email
    ) {
    }

    public record SpotifyPlaylistSummary(
        String playlistId,
        String name,
        String description,
        String ownerId,
        String ownerDisplayName,
        boolean collaborative,
        int trackCount
    ) {
    }

    public record SpotifyPlaylistTrack(
        String spotifyTrackId,
        String title,
        String artistName,
        String trackHref,
        String spotifyUri,
        Integer durationMs
    ) {
    }

    public record SpotifyAudioFeaturesSnapshot(
        String spotifyTrackId,
        String analysisUrl,
        String trackHref,
        String spotifyUri,
        String featureType,
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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyCurrentUserProfileResponse(
        String id,
        @JsonProperty("display_name") String displayName,
        String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistPageResponse(
        List<SpotifyPlaylistItemResponse> items,
        String next
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistItemResponse(
        String id,
        String name,
        String description,
        Boolean collaborative,
        SpotifyPlaylistOwnerResponse owner,
        SpotifyPlaylistTracksSummaryResponse tracks
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistOwnerResponse(
        String id,
        @JsonProperty("display_name") String displayName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistTracksSummaryResponse(
        Integer total
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistTrackPageResponse(
        List<SpotifyPlaylistTrackItemResponse> items,
        String next
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyPlaylistTrackItemResponse(
        SpotifyTrackResponse track
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyTrackResponse(
        String id,
        String name,
        String href,
        String uri,
        @JsonProperty("duration_ms") Integer durationMs,
        @JsonProperty("is_local") Boolean isLocal,
        List<SpotifyArtistResponse> artists
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyArtistResponse(
        String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyAudioFeaturesBatchResponse(
        @JsonProperty("audio_features") List<SpotifyAudioFeaturesItemResponse> audioFeatures
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyAudioFeaturesItemResponse(
        String id,
        @JsonProperty("analysis_url") String analysisUrl,
        @JsonProperty("track_href") String trackHref,
        String uri,
        @JsonProperty("type") String featureType,
        @JsonProperty("duration_ms") Integer durationMs,
        @JsonProperty("key") Integer musicalKey,
        Integer mode,
        @JsonProperty("time_signature") Integer timeSignature,
        Double acousticness,
        Double danceability,
        Double energy,
        Double instrumentalness,
        Double liveness,
        Double loudness,
        Double speechiness,
        Double tempo,
        Double valence
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyApiErrorEnvelope(
        SpotifyApiErrorResponse error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyApiErrorResponse(
        Integer status,
        String message
    ) {
    }
}
