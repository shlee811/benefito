package com.lion.benefito.infrastructure.external.seed;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.seed")
public class SeedProperties {

    // "classpath:data/seed_contest_activity.csv" 형태.
    private String csvPath;
}
