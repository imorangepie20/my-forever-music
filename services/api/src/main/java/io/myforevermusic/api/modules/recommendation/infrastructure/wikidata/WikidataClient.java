package io.myforevermusic.api.modules.recommendation.infrastructure.wikidata;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class WikidataClient {

    private static final String BASE_URL = "https://www.wikidata.org/w/api.php";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String userAgent;

    public WikidataClient(
        ObjectMapper objectMapper,
        @Value("${app.recommendation.metadata.wikidata.user-agent:MyForeverMusic/1.0 ( admin@my-forever-music.local )}") String userAgent
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.objectMapper = objectMapper;
        this.userAgent = userAgent;
    }

    public WikidataEntitySearchResponse searchEntities(String title, String artist, int limit) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required for Wikidata entity search.");
        }
        int safeLimit = Math.max(1, Math.min(25, limit));
        String search = artist == null || artist.isBlank()
            ? title.trim()
            : title.trim() + " " + artist.trim();
        String url = BASE_URL
            + "?action=wbsearchentities&format=json&language=en&type=item&limit=" + safeLimit
            + "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8);
        return send(url);
    }

    private WikidataEntitySearchResponse send(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("User-Agent", userAgent)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Wikidata responded with " + response.statusCode() + ": " + response.body()
                );
            }
            return objectMapper.readValue(response.body(), WikidataEntitySearchResponse.class);
        } catch (IOException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Wikidata response could not be parsed.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "Wikidata request was interrupted.", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WikidataEntitySearchResponse(List<WikidataEntitySearchResult> search) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WikidataEntitySearchResult(
        String id,
        String title,
        String label,
        String description,
        String concepturi
    ) {}
}
