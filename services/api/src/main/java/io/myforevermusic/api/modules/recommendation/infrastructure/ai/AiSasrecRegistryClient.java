package io.myforevermusic.api.modules.recommendation.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.gms.infrastructure.ai.AiServiceProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
public class AiSasrecRegistryClient {

    private final HttpClient httpClient;
    private final AiServiceProperties aiServiceProperties;
    private final ObjectMapper objectMapper;

    public AiSasrecRegistryClient(
        AiServiceProperties aiServiceProperties,
        ObjectMapper objectMapper
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.aiServiceProperties = aiServiceProperties;
        this.objectMapper = objectMapper;
    }

    public SasrecRegistryResponse latest(String userId) {
        String path = "/v1/recommendations/datasets/sasrec/models/latest?user_id="
            + URLEncoder.encode(userId, StandardCharsets.UTF_8);
        return send(HttpRequest.newBuilder()
            .uri(URI.create(aiServiceProperties.baseUrl() + path))
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .GET()
            .build());
    }

    public SasrecRegistryResponse promote(String userId, String modelVersion) {
        String path = "/v1/recommendations/datasets/sasrec/models/"
            + URLEncoder.encode(modelVersion, StandardCharsets.UTF_8)
            + "/promote?user_id="
            + URLEncoder.encode(userId, StandardCharsets.UTF_8);
        return send(emptyPost(path));
    }

    public SasrecRegistryResponse disable(String userId, String modelVersion) {
        String path = "/v1/recommendations/datasets/sasrec/models/"
            + URLEncoder.encode(modelVersion, StandardCharsets.UTF_8)
            + "/disable?user_id="
            + URLEncoder.encode(userId, StandardCharsets.UTF_8);
        return send(emptyPost(path));
    }

    public SasrecRegistryResponse rollback(String userId) {
        String path = "/v1/recommendations/datasets/sasrec/models/rollback?user_id="
            + URLEncoder.encode(userId, StandardCharsets.UTF_8);
        return send(emptyPost(path));
    }

    private HttpRequest emptyPost(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(aiServiceProperties.baseUrl() + path))
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    }

    private SasrecRegistryResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "AI service responded with an error for the SASRec registry call: " + response.statusCode()
                );
            }
            return objectMapper.readValue(response.body(), SasrecRegistryResponse.class);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service returned an unreadable SASRec registry response.",
                ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "SASRec registry request was interrupted.",
                ex
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecRegistryResponse(
        String service,
        String status,
        String userId,
        String modelVersion,
        String artifactDir,
        String generatedAt,
        Integer vocabularySize,
        Integer trainExampleCount,
        String datasetVersion,
        String datasetFingerprint,
        List<String> warnings
    ) {
    }
}
