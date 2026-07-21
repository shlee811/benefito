package com.lion.benefito.infrastructure.external.scholarship;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.scholarship")
public class ScholarshipProperties {

    // "classpath:data/scholarship.csv" 형태. ResourceLoader가 그대로 해석하므로 하드코딩하지 않는다.
    private String csvPath;
}
