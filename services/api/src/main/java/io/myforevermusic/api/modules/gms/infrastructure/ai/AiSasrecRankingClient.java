package io.myforevermusic.api.modules.gms.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
public class AiSasrecRankingClient {

    private final HttpClient httpClient;
    private final AiServiceProperties aiServiceProperties;
    private final ObjectMapper objectMapper;

    public AiSasrecRankingClient(
        AiServiceProperties aiServiceProperties,
        ObjectMapper objectMapper
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.aiServiceProperties = aiServiceProperties;
        this.objectMapper = objectMapper;
    }

    public Optional<SasrecRankingResponse> rankCandidates(
        String userId,
        List<String> contextTrackIds,
        List<String> candidateTrackIds,
        int limit
    ) {
        Optional<String> modelVersion = resolveModelVersion(userId);
        if (modelVersion.isEmpty()) {
            return Optional.empty();
        }

        SasrecRankingRequest request = new SasrecRankingRequest(
            modelVersion.get(),
            contextTrackIds,
            candidateTrackIds,
            Math.max(1, Math.min(100, limit))
        );

        try {
            String payload = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceProperties.baseUrl() + aiServiceProperties.sasrecRankingPath()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "AI service responded with an error while ranking SASRec candidates: " + httpResponse.statusCode()
                );
            }

            SasrecRankingResponse response = objectMapper.readValue(
                httpResponse.body(),
                SasrecRankingResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI service returned an empty SASRec ranking response.");
            }

            return Optional.of(response);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "Failed to serialize or deserialize the AI SASRec ranking payload.",
                ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI SASRec ranking request was interrupted.",
                ex
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service returned an unreadable SASRec ranking response.",
                ex
            );
        } catch (IllegalArgumentException | RestClientException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service is unreachable. Check AI_SERVICE_BASE_URL and the FastAPI process.",
                ex
            );
        }
    }

    private Optional<String> resolveModelVersion(String userId) {
        String configuredModelVersion = aiServiceProperties.sasrecModelVersion();
        if (configuredModelVersion != null && !configuredModelVersion.isBlank()) {
            return Optional.of(configuredModelVersion);
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceProperties.baseUrl() + latestModelPath(userId)))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "AI service responded with an error while resolving the latest SASRec model: " + httpResponse.statusCode()
                );
            }

            SasrecLatestModelResponse response = objectMapper.readValue(
                httpResponse.body(),
                SasrecLatestModelResponse.class
            );
            if (
                response != null
                    && "ok".equals(response.status())
                    && response.modelVersion() != null
                    && !response.modelVersion().isBlank()
            ) {
                return Optional.of(response.modelVersion());
            }
            return Optional.empty();
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "Failed to deserialize the latest SASRec model payload.",
                ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "Latest SASRec model request was interrupted.",
                ex
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service returned an unreadable latest SASRec model response.",
                ex
            );
        } catch (IllegalArgumentException | RestClientException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service is unreachable. Check AI_SERVICE_BASE_URL and the FastAPI process.",
                ex
            );
        }
    }

    private String latestModelPath(String userId) {
        String path = aiServiceProperties.sasrecLatestModelPath();
        if (userId == null || userId.isBlank()) {
            return path;
        }
        String separator = path.contains("?") ? "&" : "?";
        return path + separator + "user_id=" + URLEncoder.encode(userId, StandardCharsets.UTF_8);
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecRankingRequest(
        String modelVersion,
        List<String> contextTrackIds,
        List<String> candidateTrackIds,
        int k
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecRankingResponse(
        String service,
        String status,
        String modelVersion,
        List<SasrecRankedCandidate> rankedCandidates,
        List<String> warnings
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecRankedCandidate(
        int rank,
        String trackId,
        int itemIndex,
        double score
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecLatestModelResponse(
        String service,
        String status,
        String userId,
        String modelVersion,
        String artifactDir,
        String generatedAt,
        Integer vocabularySize,
        Integer trainExampleCount,
        List<String> warnings
    ) {
    }
}
