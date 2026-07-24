package com.lion.benefito.infrastructure.external.publicinstitutionjob;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.public-institution-job")
public class PublicInstitutionJobProperties {

    private String baseUrl;
    private String apiKey;
    private int numOfRows;
    private int maxPages;
}
