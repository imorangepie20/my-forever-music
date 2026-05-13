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
        if (token.isBlank()) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Discogs token is required. Set DISCOGS_TOKEN before running Discogs metadata lookup."
            );
        }
        int safeLimit = Math.max(1, Math.min(25, limit));
        String query = artist == null || artist.isBlank()
            ? title.trim()
            : title.trim() + " " + artist.trim();
        String url = BASE_URL
            + "/database/search?type=master&page=1&per_page=" + safeLimit
            + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        return send(url);
    }

    private DiscogsSearchResponse send(String url) {
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
            return objectMapper.readValue(response.body(), DiscogsSearchResponse.class);
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
}
