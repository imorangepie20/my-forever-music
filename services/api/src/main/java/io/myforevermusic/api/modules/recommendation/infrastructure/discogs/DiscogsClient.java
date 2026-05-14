package io.myforevermusic.api.modules.recommendation.infrastructure.discogs;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DiscogsClient {

    private static final String BASE_URL = "https://api.discogs.com";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String userAgent;
    private final String token;

    public DiscogsClient(
        ObjectMapper objectMapper,
        @Value("${app.recommendation.metadata.discogs.user-agent:MyForeverMusic/1.0}") String userAgent,
        @Value("${app.recommendation.metadata.discogs.token:}") String token
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.objectMapper = objectMapper;
        this.userAgent = userAgent;
        this.token = token == null ? "" : token.trim();
    }

    public DiscogsSearchResponse searchMasters(String title, String artist, int limit) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required for Discogs master search.");
        }
        assertTokenConfigured();
        int safeLimit = Math.max(1, Math.min(25, limit));
        String query = artist == null || artist.isBlank()
            ? title.trim()
            : title.trim() + " " + artist.trim();
        String url = BASE_URL
            + "/database/search?type=master&page=1&per_page=" + safeLimit
            + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        return send(url, DiscogsSearchResponse.class);
    }

    public DiscogsMasterDetail getMaster(int masterId) {
        assertTokenConfigured();
        if (masterId <= 0) {
            throw new IllegalArgumentException("Discogs master id must be positive.");
        }
        return send(BASE_URL + "/masters/" + masterId, DiscogsMasterDetail.class);
    }

    public DiscogsReleaseDetail getRelease(int releaseId) {
        assertTokenConfigured();
        if (releaseId <= 0) {
            throw new IllegalArgumentException("Discogs release id must be positive.");
        }
        return send(BASE_URL + "/releases/" + releaseId, DiscogsReleaseDetail.class);
    }

    private void assertTokenConfigured() {
        if (token.isBlank()) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Discogs token is required. Set DISCOGS_TOKEN before running Discogs metadata lookup."
            );
        }
    }

    private <T> T send(String url, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("User-Agent", userAgent)
            .header("Authorization", "Discogs token=" + token)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Discogs responded with " + response.statusCode() + ": " + response.body()
                );
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Discogs response could not be parsed.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "Discogs request was interrupted.", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscogsSearchResponse(List<DiscogsSearchResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscogsSearchResult(
        Integer id,
        String type,
        String title,
        String country,
        String year,
        @JsonProperty("resource_url") String resourceUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscogsMasterDetail(
        Integer id,
        String title,
        Integer year,
        @JsonProperty("main_release") Integer mainRelease,
        @JsonProperty("main_release_url") String mainReleaseUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscogsReleaseDetail(
        Integer id,
        String title,
        String country,
        String year,
        List<DiscogsReleaseLabel> labels
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscogsReleaseLabel(
        Integer id,
        String name,
        @JsonProperty("catno") String catalogNumber,
        @JsonProperty("resource_url") String resourceUrl
    ) {}
}
