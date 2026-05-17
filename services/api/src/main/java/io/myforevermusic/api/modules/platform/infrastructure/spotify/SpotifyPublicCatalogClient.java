package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Public-catalogue Spotify lookups (track search, single-track fetch) driven by
 * the app-only Client Credentials token. Used by features that surface data
 * for unauthenticated visitors — e.g. resolving a Melon Hot 100 row to a
 * playable Spotify track without requiring the user to be Spotify-connected.
 */
@Component
public class SpotifyPublicCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(SpotifyPublicCatalogClient.class);

    private final PlatformOAuthProperties platformOAuthProperties;
    private final SpotifyAppTokenService appTokenService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public SpotifyPublicCatalogClient(
        PlatformOAuthProperties platformOAuthProperties,
        SpotifyAppTokenService appTokenService,
        ObjectMapper objectMapper
    ) {
        this(platformOAuthProperties, appTokenService, objectMapper, HttpClient.newHttpClient());
    }

    SpotifyPublicCatalogClient(
        PlatformOAuthProperties platformOAuthProperties,
        SpotifyAppTokenService appTokenService,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.platformOAuthProperties = platformOAuthProperties;
        this.appTokenService = appTokenService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public record PublicTrack(
        String spotifyTrackId,
        String title,
        String artistName,
        String albumTitle,
        String imageUrl,
        String previewUrl,
        String externalUrl,
        Integer durationMs
    ) {
    }

    public List<PublicTrack> searchTracks(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int requestLimit = Math.min(Math.max(limit, 1), 50);
        String uri = "%s/search?q=%s&type=track&limit=%d".formatted(
            apiBaseUri(),
            URLEncoder.encode(query, StandardCharsets.UTF_8),
            requestLimit
        );
        try {
            SpotifySearchTrackEnvelope envelope = get(uri, SpotifySearchTrackEnvelope.class);
            SpotifySearchTrackPage trackPage = envelope.tracks();
            if (trackPage == null) {
                return List.of();
            }
            return Optional.ofNullable(trackPage.items()).orElse(List.of())
                .stream()
                .filter(item -> item != null && item.id() != null && !item.id().isBlank())
                .map(SpotifyPublicCatalogClient::toPublicTrack)
                .toList();
        } catch (IllegalArgumentException ex) {
            log.warn("Spotify public catalogue search failed (query={}): {}", query, ex.getMessage());
            return List.of();
        }
    }

    public Optional<PublicTrack> getTrack(String spotifyTrackId) {
        if (spotifyTrackId == null || spotifyTrackId.isBlank()) {
            return Optional.empty();
        }
        String uri = "%s/tracks/%s".formatted(apiBaseUri(), URLEncoder.encode(spotifyTrackId, StandardCharsets.UTF_8));
        try {
            SpotifyTrackResponse response = get(uri, SpotifyTrackResponse.class);
            if (response == null || response.id() == null || response.id().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(toPublicTrack(response));
        } catch (IllegalArgumentException ex) {
            log.warn("Spotify public catalogue track fetch failed (id={}): {}", spotifyTrackId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns the cached follower count for a Spotify playlist. Uses the
     * minimal `fields=followers.total` projection so the call stays cheap
     * enough to run in a backfill loop.
     */
    public Optional<Integer> getPlaylistFollowers(String spotifyPlaylistId) {
        if (spotifyPlaylistId == null || spotifyPlaylistId.isBlank()) {
            return Optional.empty();
        }
        String uri = "%s/playlists/%s?fields=followers.total".formatted(
            apiBaseUri(),
            URLEncoder.encode(spotifyPlaylistId, StandardCharsets.UTF_8)
        );
        try {
            SpotifyPlaylistFollowersResponse response = get(uri, SpotifyPlaylistFollowersResponse.class);
            if (response == null || response.followers() == null || response.followers().total() == null) {
                return Optional.empty();
            }
            return Optional.of(response.followers().total());
        } catch (IllegalArgumentException ex) {
            log.warn("Spotify playlist followers fetch failed (id={}): {}", spotifyPlaylistId, ex.getMessage());
            return Optional.empty();
        }
    }

    private <T> T get(String uri, Class<T> responseType) {
        String token = appTokenService.getAccessToken();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                appTokenService.invalidateCache();
                throw new IllegalArgumentException(
                    "Spotify rejected the app token (401). Will retry on next call.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(
                    "Spotify public catalogue returned HTTP %d: %s".formatted(response.statusCode(), response.body()));
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Spotify public catalogue response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Spotify public catalogue request was interrupted.", exception);
        }
    }

    private String apiBaseUri() {
        String baseUri = platformOAuthProperties.getSpotify().getApiBaseUri();
        if (baseUri.endsWith("/")) {
            return baseUri.substring(0, baseUri.length() - 1);
        }
        return baseUri;
    }

    private static PublicTrack toPublicTrack(SpotifyTrackResponse track) {
        String image = null;
        if (track.album() != null && track.album().images() != null && !track.album().images().isEmpty()) {
            image = track.album().images().get(0).url();
        }
        String externalUrl = null;
        if (track.externalUrls() != null) {
            externalUrl = track.externalUrls().spotify();
        }
        String artistName = "Unknown Artist";
        if (track.artists() != null && !track.artists().isEmpty()) {
            artistName = track.artists().get(0).name();
        }
        return new PublicTrack(
            track.id(),
            track.name() == null || track.name().isBlank() ? "Untitled" : track.name(),
            artistName,
            track.album() == null ? null : track.album().name(),
            image,
            track.previewUrl(),
            externalUrl,
            track.durationMs()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifySearchTrackEnvelope(SpotifySearchTrackPage tracks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifySearchTrackPage(List<SpotifyTrackResponse> items, Integer total, String next) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyTrackResponse(
        String id,
        String name,
        List<SpotifyArtistResponse> artists,
        SpotifyAlbumResponse album,
        @JsonProperty("preview_url") String previewUrl,
        @JsonProperty("external_urls") SpotifyExternalUrls externalUrls,
        @JsonProperty("duration_ms") Integer durationMs
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyArtistResponse(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyAlbumResponse(String id, String name, List<SpotifyImageResponse> images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyImageResponse(String url, Integer height, Integer width) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyExternalUrls(String spotify) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyPlaylistFollowersResponse(SpotifyFollowers followers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyFollowers(Integer total) {
    }
}
