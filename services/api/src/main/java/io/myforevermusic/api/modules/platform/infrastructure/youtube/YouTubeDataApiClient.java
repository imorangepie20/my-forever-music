package io.myforevermusic.api.modules.platform.infrastructure.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class YouTubeDataApiClient {

    private static final String DEFAULT_API_BASE_URI = "https://www.googleapis.com/youtube/v3";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String apiBaseUri;

    @Autowired
    public YouTubeDataApiClient(
        ObjectMapper objectMapper,
        @Value("${app.youtube.data-api-key:}") String apiKey
    ) {
        this(objectMapper, HttpClient.newHttpClient(), apiKey, DEFAULT_API_BASE_URI);
    }

    YouTubeDataApiClient(
        ObjectMapper objectMapper,
        HttpClient httpClient,
        String apiKey,
        String apiBaseUri
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiBaseUri = trimTrailingSlash(apiBaseUri);
    }

    public List<YouTubeVideoCandidate> searchEmbeddableVideos(String query, int limit) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.PRECONDITION_FAILED,
                "YOUTUBE_DATA_API_KEY is required before resolving YouTube playback targets."
            );
        }

        int safeLimit = Math.min(Math.max(limit, 1), 25);
        JsonNode searchBody = getJson("/search?part=snippet&type=video&videoEmbeddable=true&maxResults=%d&q=%s&key=%s"
            .formatted(safeLimit, encode(query), encode(apiKey)), "YouTube search");
        List<SearchHit> hits = new ArrayList<>();
        for (JsonNode item : searchBody.path("items")) {
            String videoId = text(item.path("id"), "videoId");
            if (videoId == null) {
                continue;
            }
            JsonNode snippet = item.path("snippet");
            hits.add(new SearchHit(
                videoId,
                text(snippet, "title"),
                text(snippet, "channelTitle"),
                text(snippet, "description"),
                thumbnailUrl(snippet.path("thumbnails"))
            ));
        }

        if (hits.isEmpty()) {
            return List.of();
        }

        JsonNode videosBody = getJson("/videos?part=status,contentDetails&id=%s&key=%s"
            .formatted(encode(joinVideoIds(hits)), encode(apiKey)), "YouTube video details");
        List<YouTubeVideoCandidate> candidates = new ArrayList<>();
        for (JsonNode item : videosBody.path("items")) {
            String videoId = text(item, "id");
            if (videoId == null) {
                continue;
            }
            SearchHit hit = findHit(hits, videoId);
            if (hit == null || !item.path("status").path("embeddable").asBoolean(false)) {
                continue;
            }
            candidates.add(new YouTubeVideoCandidate(
                videoId,
                hit.title(),
                hit.channelTitle(),
                hit.description(),
                hit.thumbnailUrl(),
                parseDurationMs(text(item.path("contentDetails"), "duration"))
            ));
        }
        return candidates;
    }

    private JsonNode getJson(String pathAndQuery, String context) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUri + pathAndQuery))
            .header("Accept", "application/json")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = response.body() == null || response.body().isBlank()
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = body.path("error").path("message").asText("%s request failed.".formatted(context));
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "%s request failed: %s".formatted(context, message)
                );
            }
            return body;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "%s request was interrupted.".formatted(context),
                exception
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "%s returned an unreadable response.".formatted(context),
                exception
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "%s endpoint could not be called.".formatted(context),
                exception
            );
        }
    }

    private static SearchHit findHit(List<SearchHit> hits, String videoId) {
        return hits.stream()
            .filter(hit -> hit.videoId().equals(videoId))
            .findFirst()
            .orElse(null);
    }

    private static String joinVideoIds(List<SearchHit> hits) {
        return String.join(",", hits.stream().map(SearchHit::videoId).toList());
    }

    private static Integer parseDurationMs(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Math.toIntExact(Duration.parse(value).toMillis());
        } catch (ArithmeticException | DateTimeParseException exception) {
            return null;
        }
    }

    private static String thumbnailUrl(JsonNode thumbnails) {
        String[] priorities = { "maxres", "standard", "high", "medium", "default" };
        for (String priority : priorities) {
            String url = text(thumbnails.path(priority), "url");
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isBlank() ? null : text;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_API_BASE_URI;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record SearchHit(
        String videoId,
        String title,
        String channelTitle,
        String description,
        String thumbnailUrl
    ) {
    }

    public record YouTubeVideoCandidate(
        String videoId,
        String title,
        String channelTitle,
        String description,
        String thumbnailUrl,
        Integer durationMs
    ) {
    }
}
