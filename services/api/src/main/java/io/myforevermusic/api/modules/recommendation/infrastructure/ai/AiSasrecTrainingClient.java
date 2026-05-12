package io.myforevermusic.api.modules.recommendation.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.gms.infrastructure.ai.AiServiceProperties;
import io.myforevermusic.api.modules.recommendation.presentation.RecommendationDatasetExportResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
public class AiSasrecTrainingClient {

    private final HttpClient httpClient;
    private final AiServiceProperties aiServiceProperties;
    private final ObjectMapper objectMapper;

    public AiSasrecTrainingClient(
        AiServiceProperties aiServiceProperties,
        ObjectMapper objectMapper
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.aiServiceProperties = aiServiceProperties;
        this.objectMapper = objectMapper;
    }

    public SasrecTrainingResponse train(
        RecommendationDatasetExportResponse dataset,
        SasrecTrainingOptions options
    ) {
        try {
            String payload = objectMapper.writeValueAsString(dataset);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceProperties.baseUrl() + trainingPath(options)))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "AI service responded with an error while training SASRec: " + httpResponse.statusCode()
                );
            }

            SasrecTrainingResponse response = objectMapper.readValue(
                httpResponse.body(),
                SasrecTrainingResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI service returned an empty SASRec training response.");
            }

            return response;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "Failed to serialize or deserialize the AI SASRec training payload.",
                ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI SASRec training request was interrupted.",
                ex
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service returned an unreadable SASRec training response.",
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

    private String trainingPath(SasrecTrainingOptions options) {
        String path = aiServiceProperties.sasrecTrainingPath();
        return path + "?"
            + "max_context_length=" + encode(options.maxContextLength())
            + "&k=" + encode(options.k())
            + "&epochs=" + encode(options.epochs())
            + "&hidden_size=" + encode(options.hiddenSize())
            + "&learning_rate=" + encode(options.learningRate())
            + "&persist_artifact=" + encode(options.persistArtifact());
    }

    private String encode(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

    public record SasrecTrainingOptions(
        int maxContextLength,
        int k,
        int epochs,
        int hiddenSize,
        double learningRate,
        boolean persistArtifact
    ) {
        public SasrecTrainingOptions {
            maxContextLength = Math.max(1, Math.min(50, maxContextLength));
            k = Math.max(1, Math.min(100, k));
            epochs = Math.max(1, Math.min(200, epochs));
            hiddenSize = Math.max(8, Math.min(128, hiddenSize));
            learningRate = Math.max(0.0001d, Math.min(1.0d, learningRate));
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SasrecTrainingResponse(
        String service,
        String status,
        String userId,
        String modelVersion,
        Map<String, Object> summary,
        Map<String, Object> metrics,
        Map<String, Object> baselineMetrics,
        Map<String, Object> metricDelta,
        Map<String, Object> modelArtifact,
        List<Map<String, Object>> evaluationExamples,
        List<String> warnings
    ) {
    }
}
