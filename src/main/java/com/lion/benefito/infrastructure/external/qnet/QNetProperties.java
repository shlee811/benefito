package com.lion.benefito.infrastructure.external.qnet;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.qnet")
public class QNetProperties {

    private String baseUrl;
    private String apiKey;

    // 이 목록에 있는 jmCd(종목코드)에 대해서만 getJMList(시험일정)를 호출한다.
    private List<String> targetCodes;
}
