package com.lion.benefito.infrastructure.ai.chat;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiChatClient implements AiChatClient {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String SYSTEM_PROMPT = "당신은 Benefito 서비스의 AI 어시스턴트입니다. 사용자의 질문에 친절하고 간결하게 답변하세요.";

    private final RestClient restClient;
    private final OpenAiChatProperties properties;

    public OpenAiChatClient(RestClient.Builder restClientBuilder, OpenAiChatProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    @Override
    public String generateReply(String message, Long postingId) {
        // postingId는 이번 PR에서 사용하지 않는다 (PostingRetriever 미구현).
        if (message == null || message.isBlank()) {
            throw new ChatException(ChatErrorCode.INVALID_INPUT);
        }

        OpenAiChatResponse response = callChatCompletionsApi(message);

        if (response == null) {
            throw new ChatException(ChatErrorCode.EMPTY_RESPONSE);
        }
        if (response.choices() == null || response.choices().isEmpty()) {
            throw new ChatException(ChatErrorCode.EMPTY_CHOICES);
        }

        OpenAiChatMessage replyMessage = response.choices().get(0).message();
        if (replyMessage == null || replyMessage.content() == null || replyMessage.content().isBlank()) {
            throw new ChatException(ChatErrorCode.EMPTY_CONTENT);
        }

        return replyMessage.content();
    }

    private OpenAiChatResponse callChatCompletionsApi(String message) {
        OpenAiChatRequest request = new OpenAiChatRequest(
                properties.getChatModel(),
                List.of(
                        new OpenAiChatMessage("system", SYSTEM_PROMPT),
                        new OpenAiChatMessage("user", message)
                )
        );
        try {
            return restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .body(request)
                    .retrieve()
                    .body(OpenAiChatResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ChatException(ChatErrorCode.AUTHENTICATION_FAILED);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ChatException(ChatErrorCode.RATE_LIMIT_EXCEEDED);
        } catch (RestClientException e) {
            throw new ChatException(ChatErrorCode.API_ERROR);
        }
    }
}
