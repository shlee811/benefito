package com.lion.benefito.global.config;

import com.lion.benefito.infrastructure.external.kstartup.KStartupProperties;
import com.lion.benefito.infrastructure.external.publicinstitutionjob.PublicInstitutionJobProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KStartupProperties.class, PublicInstitutionJobProperties.class})
public class ExternalApiConfig {
}
