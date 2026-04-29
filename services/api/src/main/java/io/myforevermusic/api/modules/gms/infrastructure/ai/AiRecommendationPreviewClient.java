package io.myforevermusic.api.modules.gms.infrastructure.ai;

import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewRequest;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
public class AiRecommendationPreviewClient {

    private final RestClient restClient;
    private final AiServiceProperties aiServiceProperties;

    public AiRecommendationPreviewClient(RestClient.Builder restClientBuilder, AiServiceProperties aiServiceProperties) {
        this.restClient = restClientBuilder
            .baseUrl(aiServiceProperties.baseUrl())
            .build();
        this.aiServiceProperties = aiServiceProperties;
    }

    public GmsRecommendationPreviewResponse requestPreview(GmsRecommendationPreviewRequest request) {
        try {
            GmsRecommendationPreviewResponse response = restClient.post()
                .uri(aiServiceProperties.recommendationPreviewPath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GmsRecommendationPreviewResponse.class);

            if (response == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI service returned an empty preview response.");
            }

            return response;
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service responded with an error while generating a preview: " + ex.getStatusCode().value(),
                ex
            );
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                BAD_GATEWAY,
                "AI service is unreachable. Check AI_SERVICE_BASE_URL and the FastAPI process.",
                ex
            );
        }
    }
}
