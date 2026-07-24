package com.lion.benefito.domain.posting.mapper;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
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
                .startDate(data.startDate())
                .homepageUrl(data.originalUrl())
                .qualification(data.qualification())
                .salary(data.salary())
                .employmentType(data.employmentType())
                .enterpriseType(data.enterpriseType())
                .career(data.career())
                .education(data.education())
                .sourceType(data.sourceType())
                .externalId(data.externalId())
                .contentHash(computeContentHash(data))
                .status(resolveStatus(data.deadline()))
                .build();
    }

    // 변경 여부를 판단해 변경된 경우에만 posting.updateFrom()을 호출한다. 반환값은 변경 여부.
    // contentHash만 비교하므로, 변경이 없는 대다수 항목에서는 Code 조회(카테고리/지역)가 전혀 발생하지 않는다.
    public boolean applyChanges(Posting posting, ExternalPostingData data) {
        String newHash = computeContentHash(data);
        boolean changed = !Objects.equals(posting.getContentHash(), newHash);

        if (changed) {
            Code categoryCode = resolveCategoryCode(data.sourceType(), data.category());
            Code regionCode = resolveRegionCode(data.region());
            posting.updateFrom(categoryCode, regionCode, data.title(), data.organization(), data.content(),
                    data.deadline(), data.startDate(), data.originalUrl(), data.qualification(),
                    data.salary(), data.employmentType(), data.enterpriseType(), data.career(), data.education(),
                    newHash, resolveStatus(data.deadline()));
        }

        return changed;
    }

    // 마감일이 지났으면 CLOSED, 그렇지 않거나(진행 중) 마감일 정보 자체가 없으면 ACTIVE로 본다.
    private PostingStatus resolveStatus(LocalDate deadline) {
        return (deadline != null && deadline.isBefore(LocalDate.now())) ? PostingStatus.CLOSED : PostingStatus.ACTIVE;
    }

    // 외부 데이터의 주요 필드를 이어붙여 SHA-256으로 압축한다. Code로 변환하기 전 원본 문자열을 그대로
    // 사용하므로(카테고리/지역 포함) DB 조회 없이 계산할 수 있다.
    private String computeContentHash(ExternalPostingData data) {
        String raw = String.join("|",
                nullToEmpty(data.title()),
                nullToEmpty(data.content()),
                nullToEmpty(data.organization()),
                nullToEmpty(data.originalUrl()),
                nullToEmpty(data.category()),
                nullToEmpty(data.region()),
                nullToEmpty(data.qualification()),
                nullToEmpty(data.salary()),
                nullToEmpty(data.employmentType()),
                nullToEmpty(data.enterpriseType()),
                nullToEmpty(data.career()),
                nullToEmpty(data.education()),
                String.valueOf(data.deadline()),
                String.valueOf(data.startDate()));
        return sha256Hex(raw);
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    // 대부분의 소스는 카테고리가 고정이라 sourceType만으로 결정되지만, SEED는 한 소스 안에
    // 여러 카테고리(공모전/대외활동)가 섞여 있어 원본 category 값을 그대로 조회에 사용한다.
    private Code resolveCategoryCode(String sourceType, String rawCategory) {
        String fixedCategoryCode = switch (sourceType) {
            case "KSTARTUP" -> "GOVERNMENT_SUPPORT";
            case "PUBLIC_INSTITUTION_JOB" -> "JOB";
            case "QNET" -> "CERTIFICATE";
            case "SEED_QNET_SCHEDULE" -> "CERTIFICATE";
            case "SEED_INTERN" -> "INTERN";
            case "SCHOLARSHIP" -> "SCHOLARSHIP";
            case "SEED", "RAG_TEST_SEED" -> rawCategory;
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
}
