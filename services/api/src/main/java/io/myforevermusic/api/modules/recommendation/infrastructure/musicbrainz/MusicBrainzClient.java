package io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz;

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

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * MusicBrainz Web Service v2 read-only client (recording 검색).
 *
 * docs: https://musicbrainz.org/doc/MusicBrainz_API
 * - 익명 호출 시 rate limit 1 req/sec, User-Agent 필수.
 * - title + artist 조합으로 recording 검색해 후보 MBID/ISRC 를 받는다.
 */
@Component
public class MusicBrainzClient {

    private static final String BASE_URL = "https://musicbrainz.org/ws/2";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String userAgent;

    public MusicBrainzClient(
        ObjectMapper objectMapper,
        @Value("${app.recommendation.metadata.musicbrainz.user-agent:MyForeverMusic/1.0 ( admin@my-forever-music.local )}") String userAgent
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.objectMapper = objectMapper;
        this.userAgent = userAgent;
    }

    public MusicBrainzRecordingSearchResponse searchRecordings(String title, String artist, int limit) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required for MusicBrainz recording search.");
        }
        StringBuilder query = new StringBuilder("recording:\"");
        query.append(escape(title));
        query.append('"');
        if (artist != null && !artist.isBlank()) {
            query.append(" AND artist:\"");
            query.append(escape(artist));
            query.append('"');
        }
        int safeLimit = Math.max(1, Math.min(25, limit));
        String url = BASE_URL + "/recording?fmt=json&limit=" + safeLimit + "&query="
            + URLEncoder.encode(query.toString(), StandardCharsets.UTF_8);
        return send(url);
    }

    private MusicBrainzRecordingSearchResponse send(String url) {
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
                    "MusicBrainz responded with " + response.statusCode() + ": " + response.body()
                );
            }
            return objectMapper.readValue(response.body(), MusicBrainzRecordingSearchResponse.class);
        } catch (IOException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "MusicBrainz response could not be parsed.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "MusicBrainz request was interrupted.", ex);
        }
    }

    private static String escape(String value) {
        // Lucene query escape: 큰따옴표와 백슬래시만 차단.
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MusicBrainzRecordingSearchResponse(
        Integer count,
        List<MusicBrainzRecording> recordings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MusicBrainzRecording(
        String id,
        String title,
        Integer length,
        Integer score,
        List<String> isrcs,
        @JsonProperty("artist-credit") List<MusicBrainzArtistCredit> artistCredit,
        List<MusicBrainzReleaseRef> releases
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MusicBrainzArtistCredit(
        String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MusicBrainzReleaseRef(
        String id,
        String title
    ) {}
}
