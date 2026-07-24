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

    private final RestClient restClient;
    private final OpenAiChatProperties properties;

    public OpenAiChatClient(RestClient.Builder restClientBuilder, OpenAiChatProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    // 프롬프트 조립은 ChatPromptFactory의 책임이다. 이 클래스는 전달받은 메시지를 그대로 API에 전송만 한다.
    @Override
    public String generateReply(List<OpenAiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new ChatException(ChatErrorCode.INVALID_INPUT);
        }

        OpenAiChatResponse response = callChatCompletionsApi(messages);

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

    private OpenAiChatResponse callChatCompletionsApi(List<OpenAiChatMessage> messages) {
        OpenAiChatRequest request = new OpenAiChatRequest(properties.getChatModel(), messages);
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
