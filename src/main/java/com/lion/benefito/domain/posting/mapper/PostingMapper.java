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
 * 소스별로 카테고리가 고정값이므로 sourceType -> CATEGORY 코드를 매핑해 조회한다.
 * 추후 세부 카테고리 매핑이 필요해지면 이 매핑 부분만 확장하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostingMapper {

    private final CodeRepository codeRepository;

    public Posting toNewPosting(ExternalPostingData data) {
        Code categoryCode = resolveCategoryCode(data.sourceType(), data.category());
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
            Code categoryCode = resolveCategoryCode(data.sourceType(), data.category());
            posting.updateFrom(categoryCode, regionCode, data.title(), data.organization(), data.content(),
                    data.deadline(), data.originalUrl(), data.qualification());
        }

        return changed;
    }

    // 대부분의 소스는 카테고리가 고정이라 sourceType만으로 결정되지만, SEED는 한 소스 안에
    // 여러 카테고리(공모전/대외활동)가 섞여 있어 원본 category 값을 그대로 조회에 사용한다.
    private Code resolveCategoryCode(String sourceType, String rawCategory) {
        String fixedCategoryCode = switch (sourceType) {
            case "KSTARTUP" -> "GOVERNMENT_SUPPORT";
            case "PUBLIC_INSTITUTION_JOB" -> "JOB";
            case "QNET" -> "CERTIFICATE";
            case "SCHOLARSHIP" -> "SCHOLARSHIP";
            case "SEED" -> rawCategory;
            default -> throw new ExternalPostingException(ExternalPostingErrorCode.CATEGORY_CODE_NOT_CONFIGURED);
        };

        return codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, fixedCategoryCode)
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
