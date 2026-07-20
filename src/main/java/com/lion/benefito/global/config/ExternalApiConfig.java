package com.lion.benefito.global.config;

import com.lion.benefito.infrastructure.external.kstartup.KStartupProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KStartupProperties.class)
public class ExternalApiConfig {
}
