package com.lion.benefito.infrastructure.ai.chat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiChatProperties {

    private String baseUrl;
    private String apiKey;
    private String chatModel;
}
