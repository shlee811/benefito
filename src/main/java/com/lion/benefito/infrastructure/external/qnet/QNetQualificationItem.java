package com.lion.benefito.infrastructure.external.qnet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

// 실제로 쓰는 필드만 담는다(mdobligfldcd 등 나머지는 매핑에 쓰이지 않아 제외).
public record QNetQualificationItem(
        @JacksonXmlProperty(localName = "jmcd") String jmcd,
        @JacksonXmlProperty(localName = "jmfldnm") String jmfldnm
) {
}
