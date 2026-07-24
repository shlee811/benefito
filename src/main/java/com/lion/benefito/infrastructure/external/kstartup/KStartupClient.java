package com.lion.benefito.infrastructure.external.kstartup;

import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KStartupClient {

    private static final String ANNOUNCEMENT_PATH = "/getAnnouncementInformation01";

    private final RestClient restClient;
    private final KStartupProperties properties;

    // OpenAiEmbeddingClient/OpenAiChatClient와 동일하게 공유 RestClient.Builder 빈을 그대로 사용한다.
    // (별도 timeout용 requestFactory를 여기서 재설정하면 MockRestServiceServer.bindTo(builder)가
    //  심어둔 테스트용 requestFactory를 덮어써 테스트가 깨지므로, 기존 컨벤션을 그대로 따른다.)
    public KStartupClient(RestClient.Builder restClientBuilder, KStartupProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    // ServiceKey는 항상 여기(파라미터)로만 전달되고, 요청 URL 전체를 로깅하는 코드는 어디에도 두지 않는다.
    public KStartupAnnouncementResponse fetchAnnouncements(int page, int perPage) {
        KStartupAnnouncementResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(ANNOUNCEMENT_PATH)
                            .queryParam("ServiceKey", properties.getApiKey())
                            .queryParam("page", page)
                            .queryParam("perPage", perPage)
                            .queryParam("returnType", "json")
                            .build())
                    .retrieve()
                    .body(KStartupAnnouncementResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.AUTHENTICATION_FAILED);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.RATE_LIMIT_EXCEEDED);
        } catch (RestClientException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }

        // data.go.kr 계열 API는 인증키 오류/한도초과 등에도 HTTP 200 + resultCode/resultMsg 형태의
        // 에러 본문을 내려주는 경우가 흔하다. 정상 응답에는 이 필드가 없으므로(실제 호출로 확인)
        // resultCode가 있으면 에러로 간주해 명시적으로 실패 처리한다.
        if (response != null && response.isErrorResponse()) {
            log.warn("K-Startup API 에러 응답(page={}, resultCode={}, resultMsg={})",
                    page, response.resultCode(), response.resultMsg());
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }

        return response;
    }
}
