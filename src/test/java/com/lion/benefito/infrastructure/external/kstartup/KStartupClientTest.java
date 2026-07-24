package com.lion.benefito.infrastructure.external.kstartup;

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

class KStartupClientTest {

    private static final String BASE_URL = "https://apis.data.go.kr/B552735/kisedKstartupService01";

    private MockRestServiceServer mockServer;
    private KStartupClient client;

    @BeforeEach
    void setUp() {
        KStartupProperties properties = new KStartupProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setApiKey("test-service-key");
        properties.setPerPage(50);
        properties.setMaxPages(5);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new KStartupClient(builder, properties);
    }

    @Test
    void 실제_응답_형식대로_data_배열과_totalCount를_파싱한다() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/getAnnouncementInformation01")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("ServiceKey", "test-service-key"))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("perPage", "50"))
                .andExpect(queryParam("returnType", "json"))
                .andRespond(withSuccess("""
                        {
                          "currentCount": 1,
                          "matchCount": 29492,
                          "page": 1,
                          "perPage": 50,
                          "totalCount": 29492,
                          "data": [
                            {
                              "pbanc_sn": 178584,
                              "biz_pbanc_nm": "2026 창업경진대회",
                              "pbanc_ctnt": "설명",
                              "pbanc_ntrp_nm": "BoxLadder",
                              "sprv_inst": "민간",
                              "supt_biz_clsfc": "사업화",
                              "supt_regin": "서울",
                              "aply_trgt_ctnt": "예비창업자",
                              "aply_trgt": "일반인",
                              "pbanc_rcpt_end_dt": "20260803",
                              "detl_pg_url": "https://www.k-startup.go.kr/1"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        KStartupAnnouncementResponse response = client.fetchAnnouncements(1, 50);

        assertThat(response.totalCount()).isEqualTo(29492);
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).pbancSn()).isEqualTo(178584L);
        assertThat(response.data().get(0).suptRegin()).isEqualTo("서울");
        mockServer.verify();
    }

    @Test
    void 인증_실패_401이면_AUTHENTICATION_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/getAnnouncementInformation01")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAnnouncements(1, 50))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.AUTHENTICATION_FAILED));
    }

    @Test
    void 요청_한도_초과_429이면_RATE_LIMIT_EXCEEDED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/getAnnouncementInformation01")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAnnouncements(1, 50))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 그_외_HTTP_오류면_API_CALL_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/getAnnouncementInformation01")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAnnouncements(1, 50))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }

    @Test
    void HTTP_200이지만_resultCode가_포함된_에러_응답이면_API_CALL_FAILED_예외() {
        mockServer.expect(requestTo(Matchers.startsWith(BASE_URL + "/getAnnouncementInformation01")))
                .andRespond(withSuccess("""
                        {
                          "resultCode": "30",
                          "resultMsg": "SERVICE_KEY_IS_NOT_REGISTERED_ERROR"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAnnouncements(1, 50))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.API_CALL_FAILED));
    }
}
