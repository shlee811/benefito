package com.lion.benefito.infrastructure.external.qnet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

// InquiryTestInformationNTQSVC/getJMList 실제 응답 구조(2026-07-21 실제 호출, jmCd=1320으로 확인).
public record QNetScheduleItem(
        @JacksonXmlProperty(localName = "jmfldnm") String jmfldnm,
        @JacksonXmlProperty(localName = "implplannm") String implplannm,
        @JacksonXmlProperty(localName = "docregstartdt") String docRegStartDt,
        @JacksonXmlProperty(localName = "docregenddt") String docRegEndDt,
        @JacksonXmlProperty(localName = "docexamstartdt") String docExamStartDt,
        @JacksonXmlProperty(localName = "docexamenddt") String docExamEndDt,
        @JacksonXmlProperty(localName = "docpassdt") String docPassDt,
        @JacksonXmlProperty(localName = "pracregstartdt") String pracRegStartDt,
        @JacksonXmlProperty(localName = "pracregenddt") String pracRegEndDt,
        @JacksonXmlProperty(localName = "pracexamstartdt") String pracExamStartDt,
        @JacksonXmlProperty(localName = "pracexamenddt") String pracExamEndDt,
        @JacksonXmlProperty(localName = "pracpassstartdt") String pracPassStartDt,
        @JacksonXmlProperty(localName = "pracpassenddt") String pracPassEndDt,
        @JacksonXmlProperty(localName = "obligfldnm") String obligfldnm,
        @JacksonXmlProperty(localName = "mdobligfldnm") String mdobligfldnm
) {
}
