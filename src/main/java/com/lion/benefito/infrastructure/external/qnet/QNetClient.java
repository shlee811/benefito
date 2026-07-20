package com.lion.benefito.infrastructure.external.qnet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.net.URI;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Q-Net OpenAPI는 JSON을 지원하지 않고 XML만 내려주므로(2026-07-21 실제 호출로 확인),
 * 기존 JSON 전용 공유 RestClient.Builder 빈과는 별도로 qnetRestClientBuilder 빈을 주입받아
 * XML 컨버터로만 구성한다. 다른 클라이언트(K-Startup, 공공기관 채용정보)에는 영향을 주지 않는다.
 */
@Slf4j
@Component
public class QNetClient {

    private static final String QUALIFICATION_LIST_PATH = "/InquiryListNationalQualifcationSVC/getList";
    private static final String SCHEDULE_PATH = "/InquiryTestInformationNTQSVC/getJMList";

    private final RestClient restClient;
    private final QNetProperties properties;

    // MappingJackson2XmlHttpMessageConverter(classic Jackson 2.x 기반)는 Spring Framework 7에서
    // deprecated(제거 예정)이지만, 신규 대체제(JacksonXmlHttpMessageConverter)는 별도의 Jackson 3
    // 계열(tools.jackson.*) 의존성이 필요해 프로젝트 전체의 JSON 처리(Jackson 2.x, com.fasterxml.*)와
    // 완전히 다른 스택이 된다. 이 프로젝트 전체가 Jackson 2.x 기반이라 지금은 기존 스택과 일관되게
    // 이 API를 그대로 사용한다(향후 Jackson 3 전환 시 함께 마이그레이션 필요).
    @SuppressWarnings("removal")
    public QNetClient(@Qualifier("qnetRestClientBuilder") RestClient.Builder restClientBuilder,
                       QNetProperties properties) {
        this.properties = properties;

        // Q-Net 응답 XML에는 실제로 쓰지 않는 필드가 다수 포함돼 있어, DTO에 없는 필드는 무시하도록 설정한다.
        XmlMapper xmlMapper = XmlMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new MappingJackson2XmlHttpMessageConverter(xmlMapper));
                })
                .build();
    }

    public QNetQualificationListResponse fetchQualificationList(int numOfRows, int pageNo) {
        QNetQualificationListResponse response = execute(uriBuilder -> uriBuilder
                .path(QUALIFICATION_LIST_PATH)
                .queryParam("serviceKey", properties.getApiKey())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .build(), QNetQualificationListResponse.class);

        validate(response);
        return response;
    }

    public QNetScheduleResponse fetchSchedule(String jmCd) {
        QNetScheduleResponse response = execute(uriBuilder -> uriBuilder
                .path(SCHEDULE_PATH)
                .queryParam("serviceKey", properties.getApiKey())
                .queryParam("jmCd", jmCd)
                .build(), QNetScheduleResponse.class);

        validate(response);
        return response;
    }

    private <T> T execute(Function<UriBuilder, URI> uriFunction, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.AUTHENTICATION_FAILED);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.RATE_LIMIT_EXCEEDED);
        } catch (RestClientException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }
    }

    private void validate(QNetQualificationListResponse response) {
        if (response == null || !isSuccess(response.header())) {
            logAndFail(response != null ? response.header() : null);
        }
    }

    private void validate(QNetScheduleResponse response) {
        if (response == null || !isSuccess(response.header())) {
            logAndFail(response != null ? response.header() : null);
        }
    }

    private boolean isSuccess(QNetResponseHeader header) {
        return header != null && header.isSuccess();
    }

    private void logAndFail(QNetResponseHeader header) {
        log.warn("Q-Net API 에러 응답(resultCode={}, resultMsg={})",
                header != null ? header.resultCode() : null,
                header != null ? header.resultMsg() : null);
        throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
    }
}
