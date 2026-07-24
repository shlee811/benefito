package com.lion.benefito.infrastructure.ai.embedding;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    private final RestClient restClient;
    private final OpenAiEmbeddingProperties properties;

    public OpenAiEmbeddingClient(RestClient.Builder restClientBuilder, OpenAiEmbeddingProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException(EmbeddingErrorCode.INVALID_INPUT);
        }

        OpenAiEmbeddingResponse response = callEmbeddingsApi(text);

        if (response == null) {
            throw new EmbeddingException(EmbeddingErrorCode.EMPTY_RESPONSE);
        }
        if (response.data() == null || response.data().isEmpty()) {
            throw new EmbeddingException(EmbeddingErrorCode.EMPTY_DATA);
        }

        List<Float> embedding = response.data().get(0).embedding();
        if (embedding == null || embedding.isEmpty()) {
            throw new EmbeddingException(EmbeddingErrorCode.EMPTY_EMBEDDING);
        }

        return embedding;
    }

    private OpenAiEmbeddingResponse callEmbeddingsApi(String text) {
        OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest(properties.getEmbeddingModel(), text);
        try {
            return restClient.post()
                    .uri(EMBEDDINGS_PATH)
                    .body(request)
                    .retrieve()
                    .body(OpenAiEmbeddingResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new EmbeddingException(EmbeddingErrorCode.AUTHENTICATION_FAILED);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new EmbeddingException(EmbeddingErrorCode.RATE_LIMIT_EXCEEDED);
        } catch (RestClientException e) {
            throw new EmbeddingException(EmbeddingErrorCode.API_ERROR);
        }
    }
}
