package io.myforevermusic.api.modules.ems.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.ems.application.EmsAcquisitionSignalModel;
import io.myforevermusic.api.modules.ems.application.EmsAcquisitionSignalModel.EmsAcquisitionSignal;
import io.myforevermusic.api.modules.ems.application.EmsAcquisitionSignalModel.EmsAcquisitionSignalModelRequest;
import io.myforevermusic.api.modules.ems.application.EmsAcquisitionSignalModel.EmsAcquisitionSignalModelResponse;
import io.myforevermusic.api.modules.ems.application.EmsEditorialArticle;
import io.myforevermusic.api.modules.gms.infrastructure.ai.AiServiceProperties;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
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
public class AiEmsAcquisitionSignalModel implements EmsAcquisitionSignalModel {

    private final HttpClient httpClient;
    private final AiServiceProperties aiServiceProperties;
    private final ObjectMapper objectMapper;

    public AiEmsAcquisitionSignalModel(
        AiServiceProperties aiServiceProperties,
        ObjectMapper objectMapper
    ) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        this.aiServiceProperties = aiServiceProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public EmsAcquisitionSignalModelResponse extractSignals(EmsAcquisitionSignalModelRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(AiEmsAcquisitionRequest.from(request));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceProperties.baseUrl() + aiServiceProperties.emsAcquisitionSignalsPath()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() >= 400) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "AI service responded with an error while extracting EMS acquisition signals: "
                        + httpResponse.statusCode()
                );
            }

            AiEmsAcquisitionResponse response = objectMapper.readValue(
                httpResponse.body(),
                AiEmsAcquisitionResponse.class
            );
            if (response == null) {
                throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "AI service returned an empty EMS acquisition signal response."
                );
            }
            return new EmsAcquisitionSignalModelResponse(
                response.requestId(),
                response.generatedAt(),
                response.model(),
                response.signals()
            );
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "Failed to serialize or deserialize the AI EMS acquisition payload.",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI EMS acquisition signal request was interrupted.",
                exception
            );
        } catch (ConnectException exception) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service is unreachable while extracting EMS acquisition signals. Check AI_SERVICE_BASE_URL and the FastAPI process.",
                exception
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service returned an unreadable EMS acquisition signal response.",
                exception
            );
        } catch (IllegalArgumentException | RestClientException exception) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service is unreachable. Check AI_SERVICE_BASE_URL and the FastAPI process.",
                exception
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AiEmsAcquisitionRequest(
        String sourceName,
        String sourceUrl,
        double sourceWeight,
        List<AiEmsAcquisitionArticle> articles,
        int maxSignals
    ) {
        static AiEmsAcquisitionRequest from(EmsAcquisitionSignalModelRequest request) {
            return new AiEmsAcquisitionRequest(
                request.sourceName(),
                request.sourceUrl(),
                request.sourceWeight(),
                request.articles().stream().map(AiEmsAcquisitionArticle::from).toList(),
                request.maxSignals()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AiEmsAcquisitionArticle(
        String articleUrl,
        String title,
        String summary,
        Instant publishedAt
    ) {
        static AiEmsAcquisitionArticle from(EmsEditorialArticle article) {
            return new AiEmsAcquisitionArticle(
                article.articleUrl(),
                article.title(),
                article.summary(),
                article.publishedAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AiEmsAcquisitionResponse(
        String requestId,
        Instant generatedAt,
        String service,
        String status,
        String model,
        List<EmsAcquisitionSignal> signals
    ) {
    }
}
