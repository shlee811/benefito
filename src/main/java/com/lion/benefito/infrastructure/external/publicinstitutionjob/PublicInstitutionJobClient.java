package com.lion.benefito.infrastructure.external.publicinstitutionjob;

import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class PublicInstitutionJobClient {

    private static final String LIST_PATH = "/list";

    private final RestClient restClient;
    private final PublicInstitutionJobProperties properties;

    // OpenAiEmbeddingClient/KStartupClient와 동일하게 공유 RestClient.Builder 빈을 그대로 사용한다.
    public PublicInstitutionJobClient(RestClient.Builder restClientBuilder, PublicInstitutionJobProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    // serviceKey는 항상 여기(파라미터)로만 전달되고, 요청 URL 전체를 로깅하는 코드는 어디에도 두지 않는다.
    public PublicInstitutionJobListResponse fetchList(int pageNo, int numOfRows) {
        PublicInstitutionJobListResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(LIST_PATH)
                            .queryParam("serviceKey", properties.getApiKey())
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .build())
                    .retrieve()
                    .body(PublicInstitutionJobListResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.AUTHENTICATION_FAILED);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.RATE_LIMIT_EXCEEDED);
        } catch (RestClientException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }

        // 실제 호출로 확인: 성공 시에도 실패 시에도 resultCode가 항상 존재하며, 성공값은 정수 200이다.
        if (response == null || !response.isSuccess()) {
            log.warn("공공기관 채용정보 API 에러 응답(pageNo={}, resultCode={}, resultMsg={})",
                    pageNo, response != null ? response.resultCode() : null,
                    response != null ? response.resultMsg() : null);
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }

        return response;
    }
}
