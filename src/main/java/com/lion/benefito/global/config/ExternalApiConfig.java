package com.lion.benefito.global.config;

import com.lion.benefito.infrastructure.external.kstartup.KStartupProperties;
import com.lion.benefito.infrastructure.external.publicinstitutionjob.PublicInstitutionJobProperties;
import com.lion.benefito.infrastructure.external.qnet.QNetProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({KStartupProperties.class, PublicInstitutionJobProperties.class, QNetProperties.class})
public class ExternalApiConfig {

    // Q-Net은 XML 전용이라 JSON 전용 공유 RestClient.Builder 빈(SpringAiConfig)과 별도로 둔다.
    @Bean
    public RestClient.Builder qnetRestClientBuilder() {
        return RestClient.builder();
    }
}
