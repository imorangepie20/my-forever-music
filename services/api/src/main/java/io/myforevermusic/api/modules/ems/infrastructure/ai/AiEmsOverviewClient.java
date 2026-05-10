package io.myforevermusic.api.modules.ems.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.gms.infrastructure.ai.AiServiceProperties;
import java.io.IOException;
import java.net.URI;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
public class AiEmsOverviewClient {

    private final HttpClient httpClient;
    private final AiServiceProperties aiServiceProperties;
    private final ObjectMapper objectMapper;

    public AiEmsOverviewClient(
        AiServiceProperties aiServiceProperties,
        ObjectMapper objectMapper
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.aiServiceProperties = aiServiceProperties;
        this.objectMapper = objectMapper;
    }

    public AiEmsOverviewResponse requestOverview(AiEmsOverviewRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceProperties.baseUrl() + aiServiceProperties.emsOverviewPath()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "AI service responded with an error while interpreting EMS overview: " + httpResponse.statusCode()
                );
            }

            AiEmsOverviewResponse response = objectMapper.readValue(
                httpResponse.body(),
                AiEmsOverviewResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI service returned an empty EMS overview response.");
            }

            return response;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "Failed to serialize or deserialize the AI EMS overview payload.",
                ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI EMS overview request was interrupted.",
                ex
            );
        } catch (ConnectException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service is unreachable while requesting EMS overview. Check AI_SERVICE_BASE_URL and the FastAPI process.",
                ex
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service returned an unreadable EMS overview response.",
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

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AiEmsOverviewRequest(
        String userId,
        String playlistId,
        String playlistTitle,
        int playlistCount,
        int libraryTrackCount,
        int seedTrackCount,
        int artistSeedCount,
        int genreSeedCount,
        Recommendation recommendation,
        List<Signal> topSignals,
        List<ProviderPool> providerPools,
        List<String> warnings
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Recommendation(
        String mood,
        Integer energyLevel,
        Integer familiarityBias,
        Double confidenceScore
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Signal(
        String type,
        String label,
        Double weight,
        String reason
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProviderPool(
        String platformId,
        long playlistCount,
        long trackCount,
        long audioFeatureFilledTrackCount,
        Double audioFeatureCoverageRatio,
        Instant lastCollectedAt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AiEmsOverviewResponse(
        String requestId,
        Instant generatedAt,
        String service,
        String status,
        String model,
        String tasteModelSnapshot,
        String candidateDirection,
        String readinessStatus,
        List<String> attentionItems,
        List<String> evidence,
        Double confidence
    ) {
    }
}
