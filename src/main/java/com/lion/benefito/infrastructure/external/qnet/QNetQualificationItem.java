package com.lion.benefito.infrastructure.external.qnet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

// 실제로 쓰는 필드만 담는다(mdobligfldcd 등 나머지는 매핑에 쓰이지 않아 제외).
// qualgbcd(자격구분코드: T=국가기술자격, S=국가전문자격 등)로 T/C/W/S 전체 여부를 필터링한다.
public record QNetQualificationItem(
        @JacksonXmlProperty(localName = "jmcd") String jmcd,
        @JacksonXmlProperty(localName = "jmfldnm") String jmfldnm,
        @JacksonXmlProperty(localName = "qualgbcd") String qualgbcd
) {
}
