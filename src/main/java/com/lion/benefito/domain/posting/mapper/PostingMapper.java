package com.lion.benefito.domain.posting.mapper;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ExternalPostingData(공통 모델) <-> Posting 변환을 담당한다.
 * 지금은 K-Startup만 연동하므로 카테고리를 "정부지원사업(GOVERNMENT_SUPPORT)"으로 고정한다.
 * 추후 세부 카테고리 매핑이나 다른 소스가 추가되면 이 클래스만 확장하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostingMapper {

    private static final String FIXED_CATEGORY_CODE = "GOVERNMENT_SUPPORT";

    private final CodeRepository codeRepository;

    public Posting toNewPosting(ExternalPostingData data) {
        Code categoryCode = resolveCategoryCode();
        Code regionCode = resolveRegionCode(data.region());

        return Posting.builder()
                .categoryCode(categoryCode)
                .regionCode(regionCode)
                .title(data.title())
                .company(data.organization())
                .content(data.content())
                .endDate(data.deadline())
                .homepageUrl(data.originalUrl())
                .qualification(data.qualification())
                .sourceType(data.sourceType())
                .externalId(data.externalId())
                .build();
    }

    // 변경 여부를 판단해 변경된 경우에만 posting.updateFrom()을 호출한다. 반환값은 변경 여부.
    public boolean applyChanges(Posting posting, ExternalPostingData data) {
        Code regionCode = resolveRegionCode(data.region());

        boolean changed = !Objects.equals(posting.getTitle(), data.title())
                || !Objects.equals(posting.getContent(), data.content())
                || !Objects.equals(posting.getCompany(), data.organization())
                || !Objects.equals(posting.getEndDate(), data.deadline())
                || !Objects.equals(posting.getHomepageUrl(), data.originalUrl())
                || !Objects.equals(posting.getQualification(), data.qualification())
                || !Objects.equals(codeId(posting.getRegionCode()), codeId(regionCode));

        if (changed) {
            Code categoryCode = resolveCategoryCode();
            posting.updateFrom(categoryCode, regionCode, data.title(), data.organization(), data.content(),
                    data.deadline(), data.originalUrl(), data.qualification());
        }

        return changed;
    }

    private Code resolveCategoryCode() {
        return codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, FIXED_CATEGORY_CODE)
                .orElseThrow(() -> new ExternalPostingException(ExternalPostingErrorCode.CATEGORY_CODE_NOT_CONFIGURED));
    }

    // 매핑되는 지역 코드가 없으면 null + warning 로그만 남긴다(사용자 승인된 정책).
    private Code resolveRegionCode(String rawRegion) {
        if (rawRegion == null || rawRegion.isBlank()) {
            return null;
        }

        Optional<Code> matched = codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, rawRegion);
        if (matched.isEmpty()) {
            log.warn("지역 코드 매핑 실패: '{}' 값에 대응하는 REGION 공통 코드가 없어 null로 저장합니다.", rawRegion);
        }
        return matched.orElse(null);
    }

    private Long codeId(Code code) {
        return code != null ? code.getId() : null;
    }
}
