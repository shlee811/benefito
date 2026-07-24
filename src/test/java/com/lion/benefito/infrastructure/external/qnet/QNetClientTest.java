package com.lion.benefito.infrastructure.external.qnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class QNetClientTest {

    private static final String BASE_URL = "http://openapi.q-net.or.kr/api/service/rest";

    private MockRestServiceServer mockServer;
    private QNetClient client;

    @BeforeEach
    void setUp() {
        QNetProperties properties = new QNetProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setApiKey("test-service-key");

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new QNetClient(builder, properties);
    }

    @Test
    void 종목_목록_실제_응답_형식대로_jmcd_jmfldnm_qualgbcd를_파싱한다() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/InquiryListNationalQualifcationSVC/getList")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("serviceKey", "test-service-key"))
                .andExpect(queryParam("numOfRows", "5000"))
                .andExpect(queryParam("pageNo", "1"))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <response>
                          <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
                          <body>
                            <items>
                              <item><jmcd>1320</jmcd><jmfldnm>정보처리기사</jmfldnm><qualgbcd>T</qualgbcd></item>
                              <item><jmcd>5210</jmcd><jmfldnm>공인노무사</jmfldnm><qualgbcd>S</qualgbcd></item>
                            </items>
                          </body>
                        </response>
                        """, MediaType.APPLICATION_XML));

        QNetQualificationListResponse response = client.fetchQualificationList(5000, 1);

        assertThat(response.body().getItems()).hasSize(2);
        assertThat(response.body().getItems().get(0).jmcd()).isEqualTo("1320");
        assertThat(response.body().getItems().get(0).jmfldnm()).isEqualTo("정보처리기사");
        assertThat(response.body().getItems().get(0).qualgbcd()).isEqualTo("T");
        assertThat(response.body().getItems().get(1).qualgbcd()).isEqualTo("S");
        mockServer.verify();
    }

    @Test
    void 시험일정_실제_응답_형식대로_회차별_일정을_파싱한다() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/InquiryTestInformationNTQSVC/getJMList")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("serviceKey", "test-service-key"))
                .andExpect(queryParam("jmCd", "1320"))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <response>
                          <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
                          <body>
                            <items>
                              <item>
                                <jmfldnm>정보처리기사</jmfldnm>
                                <implplannm>2026년 정기 기사 1회</implplannm>
                                <docregstartdt>20260112</docregstartdt>
                                <docregenddt>20260115</docregenddt>
                                <docexamstartdt>20260130</docexamstartdt>
                                <docexamenddt>20260303</docexamenddt>
                                <docpassdt>20260311</docpassdt>
                                <pracregstartdt>20260323</pracregstartdt>
                                <pracregenddt>20260326</pracregenddt>
                                <pracexamstartdt>20260418</pracexamstartdt>
                                <pracexamenddt>20260506</pracexamenddt>
                                <pracpassstartdt>20260612</pracpassstartdt>
                                <pracpassenddt>20260811</pracpassenddt>
                                <obligfldcd>21</obligfldcd>
                                <obligfldnm>정보통신</obligfldnm>
                                <mdobligfldcd>211</mdobligfldcd>
                                <mdobligfldnm>정보기술</mdobligfldnm>
                              </item>
                            </items>
                          </body>
                        </response>
                        """, MediaType.APPLICATION_XML));

        QNetScheduleResponse response = client.fetchSchedule("1320");

        assertThat(response.body().getItems()).hasSize(1);
        QNetScheduleItem item = response.body().getItems().get(0);
        assertThat(item.jmfldnm()).isEqualTo("정보처리기사");
        assertThat(item.implplannm()).isEqualTo("2026년 정기 기사 1회");
        assertThat(item.docRegEndDt()).isEqualTo("20260115");
        assertThat(item.obligfldnm()).isEqualTo("정보통신");
        mockServer.verify();
    }

    @Test
    void HTTP_200이지만_resultCode가_00이_아니면_API_CALL_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/InquiryTestInformationNTQSVC/getJMList")))
                .andRespond(withSuccess("""
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <response>
                          <header><resultCode>99</resultCode><resultMsg>Failed to validate a newly established connection.</resultMsg></header>
                          <body/>
                        </response>
                        """, MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client.fetchSchedule("1320"))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }

    @Test
    void 인증_실패_401이면_AUTHENTICATION_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/InquiryTestInformationNTQSVC/getJMList")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("Unauthorized")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.fetchSchedule("1320"))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.AUTHENTICATION_FAILED));
    }

    @Test
    void 요청_한도_초과_429이면_RATE_LIMIT_EXCEEDED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/InquiryTestInformationNTQSVC/getJMList")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("<response/>")
                        .contentType(MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client.fetchSchedule("1320"))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 그_외_HTTP_오류면_API_CALL_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/InquiryTestInformationNTQSVC/getJMList")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("<response/>")
                        .contentType(MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client.fetchSchedule("1320"))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }
}
