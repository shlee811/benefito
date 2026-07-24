package com.lion.benefito.infrastructure.ai.embedding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiEmbeddingProperties {

    private String baseUrl;
    private String apiKey;
    private String embeddingModel;
}
