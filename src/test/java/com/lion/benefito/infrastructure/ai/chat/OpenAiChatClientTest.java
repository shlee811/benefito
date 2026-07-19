package com.lion.benefito.infrastructure.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
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

class OpenAiChatClientTest {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final List<OpenAiChatMessage> SAMPLE_MESSAGES = List.of(
            new OpenAiChatMessage("system", "당신은 상담 챗봇입니다."),
            new OpenAiChatMessage("user", "안녕")
    );

    private MockRestServiceServer mockServer;
    private OpenAiChatClient client;

    @BeforeEach
    void setUp() {
        OpenAiChatProperties properties = new OpenAiChatProperties();
        properties.setBaseUrl("https://api.openai.com");
        properties.setApiKey("test-api-key");
        properties.setChatModel("gpt-4.1-mini");

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiChatClient(builder, properties);
    }

    @Test
    void 정상_응답이면_첫_번째_choice의_content를_반환한다() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            { "message": { "role": "assistant", "content": "안녕하세요! 무엇을 도와드릴까요?" } }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String reply = client.generateReply(SAMPLE_MESSAGES);

        assertThat(reply).isEqualTo("안녕하세요! 무엇을 도와드릴까요?");
        mockServer.verify();
    }

    @Test
    void 전달받은_메시지를_그대로_전송한다() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("안녕")))
                .andRespond(withSuccess("""
                        {"choices": [{"message": {"role": "assistant", "content": "응답"}}]}
                        """, MediaType.APPLICATION_JSON));

        String reply = client.generateReply(SAMPLE_MESSAGES);

        assertThat(reply).isEqualTo("응답");
    }

    @Test
    void messages가_null이면_INVALID_INPUT_예외() {
        assertThatThrownBy(() -> client.generateReply(null))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.INVALID_INPUT));
    }

    @Test
    void messages가_빈_리스트면_INVALID_INPUT_예외() {
        assertThatThrownBy(() -> client.generateReply(List.of()))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.INVALID_INPUT));
    }

    @Test
    void 인증_실패_401이면_AUTHENTICATION_FAILED_예외() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\": {\"message\": \"invalid api key\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateReply(SAMPLE_MESSAGES))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.AUTHENTICATION_FAILED));
    }

    @Test
    void 요청_한도_초과_429이면_RATE_LIMIT_EXCEEDED_예외() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\": {\"message\": \"rate limit\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateReply(SAMPLE_MESSAGES))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 그_외_HTTP_오류면_API_ERROR_예외() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\": {\"message\": \"server error\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateReply(SAMPLE_MESSAGES))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.API_ERROR));
    }

    @Test
    void choices_배열이_비어있으면_EMPTY_CHOICES_예외() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andRespond(withSuccess("{\"choices\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateReply(SAMPLE_MESSAGES))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.EMPTY_CHOICES));
    }

    @Test
    void choices가_없으면_EMPTY_CHOICES_예외() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateReply(SAMPLE_MESSAGES))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.EMPTY_CHOICES));
    }

    @Test
    void 메시지_content가_비어있으면_EMPTY_CONTENT_예외() {
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
                .andRespond(withSuccess("""
                        {"choices": [{"message": {"role": "assistant", "content": ""}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateReply(SAMPLE_MESSAGES))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getCode())
                        .isEqualTo(ChatErrorCode.EMPTY_CONTENT));
    }
}
