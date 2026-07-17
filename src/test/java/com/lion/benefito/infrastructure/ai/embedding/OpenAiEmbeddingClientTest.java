package com.lion.benefito.infrastructure.ai.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiEmbeddingClientTest {

    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    private MockRestServiceServer mockServer;
    private OpenAiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        OpenAiEmbeddingProperties properties = new OpenAiEmbeddingProperties();
        properties.setBaseUrl("https://api.openai.com");
        properties.setApiKey("test-api-key");
        properties.setEmbeddingModel("text-embedding-3-small");

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiEmbeddingClient(builder, properties);
    }

    @Test
    void 정상_응답이면_첫_번째_data의_embedding_벡터를_반환한다() {
        mockServer.expect(requestTo(EMBEDDINGS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "object": "list",
                          "data": [
                            { "object": "embedding", "embedding": [0.1, 0.2, 0.3], "index": 0 }
                          ],
                          "model": "text-embedding-3-small"
                        }
                        """, MediaType.APPLICATION_JSON));

        List<Float> result = client.embed("서울 지역 장학금을 추천해줘");

        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f);
        mockServer.verify();
    }

    @Test
    void 입력값이_null이면_INVALID_INPUT_예외() {
        assertThatThrownBy(() -> client.embed(null))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.INVALID_INPUT));

        mockServer.verify();
    }

    @Test
    void 입력값이_blank이면_INVALID_INPUT_예외() {
        assertThatThrownBy(() -> client.embed("   "))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.INVALID_INPUT));

        mockServer.verify();
    }

    @Test
    void 인증_실패_401이면_AUTHENTICATION_FAILED_예외() {
        mockServer.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\": {\"message\": \"invalid api key\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.AUTHENTICATION_FAILED));
    }

    @Test
    void 요청_한도_초과_429이면_RATE_LIMIT_EXCEEDED_예외() {
        mockServer.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\": {\"message\": \"rate limit exceeded\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 그_외_HTTP_오류면_API_ERROR_예외() {
        mockServer.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\": {\"message\": \"server error\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.API_ERROR));
    }

    @Test
    void data_배열이_비어있으면_EMPTY_DATA_예외() {
        mockServer.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withSuccess("{\"data\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.EMPTY_DATA));
    }

    @Test
    void data가_없으면_EMPTY_DATA_예외() {
        mockServer.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.EMPTY_DATA));
    }

    @Test
    void embedding_벡터가_비어있으면_EMPTY_EMBEDDING_예외() {
        mockServer.expect(requestTo(EMBEDDINGS_URL))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            { "object": "embedding", "embedding": [], "index": 0 }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .satisfies(ex -> assertThat(((EmbeddingException) ex).getCode())
                        .isEqualTo(EmbeddingErrorCode.EMPTY_EMBEDDING));
    }
}
