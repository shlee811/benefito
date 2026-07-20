package com.lion.benefito.infrastructure.external.kstartup;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.kstartup")
public class KStartupProperties {

    private String baseUrl;
    private String apiKey;
    private int perPage;
    private int maxPages;
}
