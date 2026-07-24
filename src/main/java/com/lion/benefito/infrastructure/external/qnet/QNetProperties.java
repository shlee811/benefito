package com.lion.benefito.infrastructure.external.qnet;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.qnet")
public class QNetProperties {

    private String baseUrl;
    private String apiKey;
}
