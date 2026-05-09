package io.myforevermusic.api.modules.platform.infrastructure.tidal;

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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TIDAL Open API v2 Web Client
 *
 * <p>Implements core API calls for PMS playlist import:
 * <ul>
 *   <li>GET /userCollectionPlaylists - user's playlists</li>
 *   <li>GET /playlists/{id} - playlist details with items</li>
 *   <li>GET /tracks/{id} - track details</li>
 * </ul>
 *
 * <p>See: https://developer.tidal.com
 */
@Component
public class TidalWebApiClient {

    private static final Logger log = LoggerFactory.getLogger(TidalWebApiClient.class);
    private static final String ACCEPT_HEADER = "application/vnd.api+json";

    private final PlatformOAuthProperties platformOAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiBaseUri;

    @Autowired
    public TidalWebApiClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper
    ) {
        this(
            platformOAuthProperties,
            objectMapper,
            HttpClient.newHttpClient(),
            platformOAuthProperties.getTidal().getApiBaseUri()
        );
    }

    TidalWebApiClient(
        PlatformOAuthProperties platformOAuthProperties,
        ObjectMapper objectMapper,
        HttpClient httpClient,
        String apiBaseUri
    ) {
        this.platformOAuthProperties = platformOAuthProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.apiBaseUri = trimTrailingSlash(apiBaseUri);
    }

    /**
     * Get the current user's profile from TIDAL.
     */
    public TidalUserProfile getCurrentUserProfile(PlatformAccountCredential credential) {
        Optional<TidalUserProfile> tokenProfile = profileFromAccessToken(credential.accessToken());
        if (tokenProfile.isPresent()) {
            return tokenProfile.get();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("%s/user".formatted(apiBaseUri)))
                .header("Accept", ACCEPT_HEADER)
                .header("Authorization", "Bearer %s".formatted(credential.accessToken()))
                .header("Content-Type", "application/vnd.api+json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("TIDAL user profile request failed: %s".formatted(response.statusCode()));
            }

            JsonApiRoot jsonApi = objectMapper.readValue(response.body(), JsonApiRoot.class);
            return Optional.ofNullable(jsonApi.data())
                .filter(data -> "users".equals(data.type()))
                .map(data -> new TidalUserProfile(
                    data.id(),
                    extractAttribute(data.attributes(), "userId", String.class),
                    extractAttribute(data.attributes(), "firstName", String.class),
                    extractAttribute(data.attributes(), "lastName", String.class),
                    extractAttribute(data.attributes(), "email", String.class)
                ))
                .orElseThrow(() -> new IllegalArgumentException("TIDAL user profile response missing user data"));
        } catch (IOException exception) {
            throw new IllegalStateException("TIDAL user profile response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TIDAL user profile request was interrupted.", exception);
        }
    }

    private Optional<TidalUserProfile> profileFromAccessToken(String accessToken) {
        return claimsFromAccessToken(accessToken)
            .flatMap(claims -> {
                String userId = firstNonBlank(
                    claimAsString(claims, "uid"),
                    claimAsString(claims, "user_id"),
                    claimAsString(claims, "userId"),
                    claimAsString(claims, "tidalUserId"),
                    claimAsString(claims, "sub")
                );
                if (userId == null) {
                    return Optional.empty();
                }

                return Optional.of(new TidalUserProfile(
                    userId,
                    claimAsString(claims, "tidalUserId"),
                    claimAsString(claims, "firstName"),
                    claimAsString(claims, "lastName"),
                    claimAsString(claims, "email")
                ));
            });
    }

    private String claimAsString(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }

        String stringValue = value.toString().trim();
        return stringValue.isBlank() ? null : stringValue;
    }

    private Optional<Map<String, Object>> claimsFromAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }

        String[] jwtParts = accessToken.split("\\.");
        if (jwtParts.length < 2) {
            return Optional.empty();
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            return Optional.of(claims);
        } catch (RuntimeException | IOException exception) {
            return Optional.empty();
        }
    }

    private String countryCodeForCredential(PlatformAccountCredential credential) {
        return firstNonBlank(
            countryCodeFromAccessToken(credential.accessToken()),
            platformOAuthProperties.getTidal().getCountryCode()
        );
    }

    private String countryCodeFromAccessToken(String accessToken) {
        return claimsFromAccessToken(accessToken)
            .map(claims -> claimAsString(claims, "cc"))
            .orElse(null);
    }

    /**
     * Get user's playlists from TIDAL.
     */
    public List<TidalPlaylistSummary> getUserPlaylists(PlatformAccountCredential credential) {
        String countryCode = countryCodeForCredential(credential);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("%s/userCollectionPlaylists?countryCode=%s&limit=50".formatted(apiBaseUri, countryCode)))
                .header("Accept", ACCEPT_HEADER)
                .header("Authorization", "Bearer %s".formatted(credential.accessToken()))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("TIDAL playlists request failed: {}", response.statusCode());
                return List.of();
            }

            JsonApiArrayRoot jsonApi = objectMapper.readValue(response.body(), JsonApiArrayRoot.class);
            return Optional.ofNullable(jsonApi.data())
                .stream()
                .flatMap(List::stream)
                .filter(data -> "playlists".equals(data.type()))
                .map(this::toPlaylistSummary)
                .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("TIDAL playlists response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TIDAL playlists request was interrupted.", exception);
        }
    }

    /**
     * Get playlist items (tracks) from TIDAL.
     */
    public List<TidalPlaylistTrack> getPlaylistTracks(
        PlatformAccountCredential credential,
        String playlistId
    ) {
        String countryCode = countryCodeForCredential(credential);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                    "%s/playlists/%s?countryCode=%s&include=items,items.artists,items.albums".formatted(
                        apiBaseUri,
                        playlistId,
                        countryCode
                    )
                ))
                .header("Accept", ACCEPT_HEADER)
                .header("Authorization", "Bearer %s".formatted(credential.accessToken()))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("TIDAL playlist tracks request failed: %s".formatted(response.statusCode()));
            }

            JsonApiWithIncluded jsonApi = objectMapper.readValue(response.body(), JsonApiWithIncluded.class);
            Map<String, JsonApiData> includedByKey = indexIncluded(jsonApi.included());

            // Get tracks from included items
            return Optional.ofNullable(jsonApi.included())
                .stream()
                .flatMap(List::stream)
                .filter(data -> "items".equals(data.type()))
                .map(item -> toPlaylistTrack(item, includedByKey))
                .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("TIDAL playlist tracks response could not be parsed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TIDAL playlist tracks request was interrupted.", exception);
        }
    }

    private TidalPlaylistSummary toPlaylistSummary(JsonApiData data) {
        Map<String, Object> attrs = data.attributes();
        return new TidalPlaylistSummary(
            data.id(),
            extractAttribute(attrs, "title", String.class),
            extractAttribute(attrs, "description", String.class),
            extractAttribute(attrs, "numberOfTracks", Integer.class, 0),
            extractAttribute(attrs, "imageId", String.class),
            buildImageUrl(extractAttribute(attrs, "imageId", String.class)),
            extractAttribute(attrs, "url", String.class),
            extractAttribute(attrs, "uuid", String.class)
        );
    }

    private TidalPlaylistTrack toPlaylistTrack(
        JsonApiData item,
        Map<String, JsonApiData> includedByKey
    ) {
        String trackId = extractSingleRelationshipId(item.relationships(), "track");
        JsonApiData trackData = includedByKey.get(resourceKey("tracks", trackId));
        Map<String, Object> trackAttributes = trackData == null ? null : trackData.attributes();
        Map<String, Object> trackRelationships = trackData == null ? null : trackData.relationships();
        List<String> artistIds = extractRelationshipIds(trackRelationships, "artists");
        List<String> albumIds = extractRelationshipIds(trackRelationships, "albums");

        String artistName = artistIds.stream()
            .map(artistId -> includedByKey.get(resourceKey("artists", artistId)))
            .filter(Objects::nonNull)
            .map(JsonApiData::attributes)
            .map(attributes -> firstNonBlank(
                extractAttribute(attributes, "name", String.class),
                extractAttribute(attributes, "artistName", String.class)
            ))
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .reduce((left, right) -> left + ", " + right)
            .orElse("TIDAL Artist");

        JsonApiData albumData = albumIds.stream()
            .map(albumId -> includedByKey.get(resourceKey("albums", albumId)))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        Map<String, Object> albumAttributes = albumData == null ? null : albumData.attributes();

        Integer durationSeconds = extractAttribute(trackAttributes, "duration", Integer.class, 0);
        String albumImageId = firstNonBlank(
            extractAttribute(albumAttributes, "imageId", String.class),
            extractAttribute(albumAttributes, "cover", String.class),
            extractAttribute(albumAttributes, "coverImageId", String.class)
        );
        String externalUrl = firstNonBlank(
            extractAttribute(trackAttributes, "url", String.class),
            extractAttribute(trackAttributes, "shareUrl", String.class),
            extractAttribute(trackAttributes, "externalUrl", String.class)
        );
        String previewUrl = firstNonBlank(
            extractAttribute(trackAttributes, "previewUrl", String.class),
            extractAttribute(trackAttributes, "previewURL", String.class)
        );

        return new TidalPlaylistTrack(
            trackId,
            firstNonBlank(extractAttribute(trackAttributes, "title", String.class), "Unknown Track"),
            artistName,
            firstNonBlank(
                extractAttribute(albumAttributes, "title", String.class),
                extractAttribute(trackAttributes, "albumTitle", String.class),
                "TIDAL Album"
            ),
            buildImageUrl(albumImageId),
            externalUrl,
            trackId == null || trackId.isBlank() ? null : "tidal:track:%s".formatted(trackId),
            previewUrl,
            normalizeIsrc(extractAttribute(trackAttributes, "isrc", String.class)),
            durationSeconds == null ? 0 : durationSeconds * 1000
        );
    }

    private String buildImageUrl(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return null;
        }
        if (imageId.contains("-")) {
            return "https://resources.tidal.com/images/%s/750x750.jpg".formatted(imageId.replace("-", "/"));
        }
        if (imageId.length() < 12) {
            return null;
        }
        // TIDAL image URL pattern (may need adjustment based on actual API)
        return "https://resources.tidal.com/images/%s/%s/%s/750x750.jpg".formatted(
            imageId.substring(0, 4),
            imageId.substring(4, 8),
            imageId.substring(8, 12)
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T extractAttribute(Map<String, Object> attributes, String key, Class<T> type) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        // Handle numeric conversion
        if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (type == String.class && value != null) {
            return (T) value.toString();
        }
        return null;
    }

    private <T> T extractAttribute(Map<String, Object> attributes, String key, Class<T> type, T defaultValue) {
        T value = extractAttribute(attributes, key, type);
        return value != null ? value : defaultValue;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://openapi.tidal.com/v2";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private Map<String, JsonApiData> indexIncluded(List<JsonApiData> included) {
        Map<String, JsonApiData> indexed = new LinkedHashMap<>();
        if (included == null) {
            return indexed;
        }

        for (JsonApiData data : included) {
            if (data == null || data.type() == null || data.id() == null) {
                continue;
            }
            indexed.put(resourceKey(data.type(), data.id()), data);
        }
        return indexed;
    }

    private String resourceKey(String type, String id) {
        return type + ":" + id;
    }

    private String extractSingleRelationshipId(Map<String, Object> relationships, String relationshipName) {
        List<String> ids = extractRelationshipIds(relationships, relationshipName);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private List<String> extractRelationshipIds(Map<String, Object> relationships, String relationshipName) {
        if (relationships == null) {
            return List.of();
        }
        Object relationship = relationships.get(relationshipName);
        if (!(relationship instanceof Map<?, ?> relationshipMap)) {
            return List.of();
        }
        Object data = relationshipMap.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object id = dataMap.get("id");
            return id == null ? List.of() : List.of(id.toString());
        }
        if (!(data instanceof List<?> dataList)) {
            return List.of();
        }
        return dataList.stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(map -> map.get("id"))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .toList();
    }

    private String normalizeIsrc(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    // JSON:API Response Records

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonApiRoot(
        @JsonProperty("data") JsonApiData data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonApiArrayRoot(
        @JsonProperty("data") List<JsonApiData> data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonApiWithIncluded(
        @JsonProperty("data") JsonApiData data,
        @JsonProperty("included") List<JsonApiData> included
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonApiData(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("attributes") Map<String, Object> attributes,
        @JsonProperty("relationships") Map<String, Object> relationships
    ) {}

    // Public Data Models

    public record TidalUserProfile(
        String userId,
        String tidalUserId,
        String firstName,
        String lastName,
        String email
    ) {}

    public record TidalPlaylistSummary(
        String playlistId,
        String name,
        String description,
        int trackCount,
        String imageId,
        String coverImageUrl,
        String externalUrl,
        String uuid
    ) {}

    public record TidalPlaylistTrack(
        String tidalTrackId,
        String title,
        String artistName,
        String albumTitle,
        String albumImageUrl,
        String externalUrl,
        String tidalUri,
        String previewUrl,
        String isrc,
        int durationMs
    ) {}
}
