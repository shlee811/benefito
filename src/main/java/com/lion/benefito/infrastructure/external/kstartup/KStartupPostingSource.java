package com.lion.benefito.infrastructure.external.kstartup;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KStartupPostingSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "KSTARTUP";
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final KStartupClient kStartupClient;
    private final KStartupProperties properties;

    @Override
    public List<ExternalPostingData> fetchPostings() {
        List<ExternalPostingData> result = new ArrayList<>();
        int perPage = properties.getPerPage();
        int maxPages = properties.getMaxPages();

        for (int page = 1; page <= maxPages; page++) {
            KStartupAnnouncementResponse response;
            try {
                response = kStartupClient.fetchAnnouncements(page, perPage);
            } catch (Exception e) {
                log.warn("K-Startup {}페이지 조회 실패, 지금까지 수집한 {}건으로 수집을 중단합니다: {}",
                        page, result.size(), e.getMessage());
                break;
            }

            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }

            for (KStartupAnnouncementItem item : response.data()) {
                try {
                    result.add(toExternalPostingData(item));
                } catch (Exception e) {
                    log.warn("K-Startup 공고 파싱 실패(pbanc_sn={}): {}", item.pbancSn(), e.getMessage());
                }
            }

            boolean isLastPage = (long) page * perPage >= response.totalCount();
            if (isLastPage) {
                break;
            }
        }

        return result;
    }

    private ExternalPostingData toExternalPostingData(KStartupAnnouncementItem item) {
        if (item.pbancSn() == null) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }

        return new ExternalPostingData(
                SOURCE_TYPE,
                String.valueOf(item.pbancSn()),
                item.bizPbancNm(),
                item.pbancCtnt(),
                resolveOrganization(item),
                item.detlPgUrl(),
                item.suptBizClsfc(),
                item.suptRegin(),
                buildQualification(item),
                parseDeadline(item.pbancRcptEndDt())
        );
    }

    // 사용자 결정: company는 pbanc_ntrp_nm(창업지원기관명)을 우선 사용하고, 값이 없으면 sprv_inst(주관기관)를 사용한다.
    private String resolveOrganization(KStartupAnnouncementItem item) {
        if (item.pbancNtrpNm() != null && !item.pbancNtrpNm().isBlank()) {
            return item.pbancNtrpNm();
        }
        return item.sprvInst();
    }

    // 사용자 결정: qualification은 aply_trgt_ctnt를 기본으로 하고, aply_trgt가 있으면 함께 조합한다.
    private String buildQualification(KStartupAnnouncementItem item) {
        String base = item.aplyTrgtCtnt() != null ? item.aplyTrgtCtnt() : "";
        String targetTypes = item.aplyTrgt();
        if (targetTypes == null || targetTypes.isBlank()) {
            return base;
        }
        return base.isBlank() ? "신청 대상: " + targetTypes : base + "\n신청 대상: " + targetTypes;
    }

    // 실제 응답 확인 결과 pbanc_rcpt_end_dt는 "20260803"과 같은 yyyyMMdd 8자리 숫자 문자열이다
    // (문서의 XML 예시 "2012-12-01 00:00:00" 형식은 실제 API와 다르다).
    private LocalDate parseDeadline(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
        try {
            return LocalDate.parse(rawDate, DEADLINE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.INVALID_RESPONSE);
        }
    }
}
