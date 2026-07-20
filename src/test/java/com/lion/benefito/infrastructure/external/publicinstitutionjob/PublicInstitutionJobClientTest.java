package com.lion.benefito.infrastructure.external.publicinstitutionjob;

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

class PublicInstitutionJobClientTest {

    private static final String BASE_URL = "https://apis.data.go.kr/1051000/recruitment";

    private MockRestServiceServer mockServer;
    private PublicInstitutionJobClient client;

    @BeforeEach
    void setUp() {
        PublicInstitutionJobProperties properties = new PublicInstitutionJobProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setApiKey("test-service-key");
        properties.setNumOfRows(100);
        properties.setMaxPages(5);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new PublicInstitutionJobClient(builder, properties);
    }

    @Test
    void 실제_응답_형식대로_result_배열과_totalCount를_파싱한다() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/list")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("serviceKey", "test-service-key"))
                .andExpect(queryParam("pageNo", "1"))
                .andExpect(queryParam("numOfRows", "100"))
                .andRespond(withSuccess("""
                        {
                          "resultCode": 200,
                          "resultMsg": "성공했습니다.",
                          "totalCount": 111983,
                          "result": [
                            {
                              "recrutPblntSn": 302908,
                              "recrutPbancTtl": "2026년도 제3차 혈액관리본부 전문직 등 신규 직원 공개 모집",
                              "instNm": "대한적십자사",
                              "pbancEndYmd": "20260804",
                              "srcUrl": "https://bloodinfo.careeron.co.kr/",
                              "workRgnNmLst": "서울",
                              "hireTypeNmLst": "정규직",
                              "acbgCondNmLst": "학력무관",
                              "aplyQlfcCn": "예비창업자",
                              "scrnprcdrMthdExpln": "서류전형",
                              "prefCn": "취업지원 대상자",
                              "prefCondCn": "장애인",
                              "ncsCdNmLst": "보건.의료",
                              "recrutSeNm": "신입"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        PublicInstitutionJobListResponse response = client.fetchList(1, 100);

        assertThat(response.totalCount()).isEqualTo(111983);
        assertThat(response.result()).hasSize(1);
        assertThat(response.result().get(0).recrutPblntSn()).isEqualTo(302908L);
        assertThat(response.result().get(0).instNm()).isEqualTo("대한적십자사");
        mockServer.verify();
    }

    @Test
    void 인증_실패_401이면_AUTHENTICATION_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/list")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("Unauthorized")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.fetchList(1, 100))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.AUTHENTICATION_FAILED));
    }

    @Test
    void 요청_한도_초과_429이면_RATE_LIMIT_EXCEEDED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/list")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchList(1, 100))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 그_외_HTTP_오류면_API_CALL_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/list")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchList(1, 100))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }

    @Test
    void HTTP_200이지만_resultCode가_200이_아니면_API_CALL_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/list")))
                .andRespond(withSuccess("""
                        {
                          "resultCode": 30,
                          "resultMsg": "SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
                          "totalCount": 0,
                          "result": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchList(1, 100))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }

    @Test
    void resultCode가_200이면서_result가_빈배열이면_정상으로_처리한다() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/list")))
                .andRespond(withSuccess("""
                        {
                          "resultCode": 200,
                          "resultMsg": "성공했습니다.",
                          "totalCount": 111983,
                          "result": []
                        }
                        """, MediaType.APPLICATION_JSON));

        PublicInstitutionJobListResponse response = client.fetchList(999999, 100);

        assertThat(response.result()).isEmpty();
        assertThat(response.totalCount()).isEqualTo(111983);
    }
}
