package com.lion.benefito.global.config;

import com.lion.benefito.infrastructure.external.kstartup.KStartupProperties;
import com.lion.benefito.infrastructure.external.publicinstitutionjob.PublicInstitutionJobProperties;
import com.lion.benefito.infrastructure.external.qnet.QNetProperties;
import com.lion.benefito.infrastructure.external.scholarship.ScholarshipProperties;
import com.lion.benefito.infrastructure.external.seed.SeedProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({KStartupProperties.class, PublicInstitutionJobProperties.class, QNetProperties.class,
        ScholarshipProperties.class, SeedProperties.class})
public class ExternalApiConfig {

    // Q-Net은 XML 전용이라 JSON 전용 공유 RestClient.Builder 빈(SpringAiConfig)과 별도로 둔다.
    // SpringAiConfig.restClientBuilder()와 동일한 이유로 connect/read timeout을 명시한다.
    @Bean
    public RestClient.Builder qnetRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
