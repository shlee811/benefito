package com.lion.benefito.domain.posting.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostingMapperTest {

    private CodeRepository codeRepository;
    private PostingMapper postingMapper;
    private Code governmentSupportCode;
    private Code seoulCode;

    @BeforeEach
    void setUp() {
        codeRepository = mock(CodeRepository.class);
        postingMapper = new PostingMapper(codeRepository);

        governmentSupportCode = mock(Code.class);
        when(governmentSupportCode.getId()).thenReturn(100L);
        seoulCode = mock(Code.class);
        when(seoulCode.getId()).thenReturn(200L);

        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "GOVERNMENT_SUPPORT"))
                .thenReturn(Optional.of(governmentSupportCode));
    }

    private ExternalPostingData data(String region, String qualification, LocalDate deadline) {
        return new ExternalPostingData("KSTARTUP", "178584", "제목", "내용", "회사",
                "https://k-startup.go.kr/1", "사업화", region, qualification, deadline);
    }

    private ExternalPostingData publicInstitutionJobData(String region, LocalDate deadline) {
        return new ExternalPostingData("PUBLIC_INSTITUTION_JOB", "302908", "제목", "내용", "기관",
                "https://example.com/1", null, region, "학력무관", deadline);
    }

    @Test
    void 신규_공고는_카테고리가_GOVERNMENT_SUPPORT로_고정되고_지역명이_일치하면_regionCode가_매핑된다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));

        Posting posting = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.of(2026, 8, 3)));

        assertThat(posting.getCategoryCode()).isEqualTo(governmentSupportCode);
        assertThat(posting.getRegionCode()).isEqualTo(seoulCode);
        assertThat(posting.getSourceType()).isEqualTo("KSTARTUP");
        assertThat(posting.getExternalId()).isEqualTo("178584");
    }

    @Test
    void 매핑되는_지역_코드가_없으면_regionCode는_null이다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "전남광주"))
                .thenReturn(Optional.empty());

        Posting posting = postingMapper.toNewPosting(data("전남광주", "누구나", LocalDate.of(2026, 8, 3)));

        assertThat(posting.getRegionCode()).isNull();
    }

    @Test
    void GOVERNMENT_SUPPORT_공통코드가_없으면_예외() {
        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "GOVERNMENT_SUPPORT"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postingMapper.toNewPosting(data("서울", "누구나", LocalDate.of(2026, 8, 3))))
                .isInstanceOf(ExternalPostingException.class)
                .satisfies(ex -> assertThat(((ExternalPostingException) ex).getCode())
                        .isEqualTo(ExternalPostingErrorCode.CATEGORY_CODE_NOT_CONFIGURED));
    }

    @Test
    void PUBLIC_INSTITUTION_JOB_소스는_카테고리가_JOB으로_고정된다() {
        Code jobCode = mock(Code.class);
        when(jobCode.getId()).thenReturn(300L);
        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB"))
                .thenReturn(Optional.of(jobCode));
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "전국"))
                .thenReturn(Optional.empty());

        Posting posting = postingMapper.toNewPosting(
                publicInstitutionJobData("전국", LocalDate.of(2026, 8, 4)));

        assertThat(posting.getCategoryCode()).isEqualTo(jobCode);
        assertThat(posting.getSourceType()).isEqualTo("PUBLIC_INSTITUTION_JOB");
        assertThat(posting.getRegionCode()).isNull();
    }

    @Test
    void 변경된_필드가_없으면_updateFrom을_호출하지_않고_false를_반환한다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));
        Posting existing = Posting.builder()
                .categoryCode(governmentSupportCode)
                .regionCode(seoulCode)
                .title("제목")
                .company("회사")
                .content("내용")
                .endDate(LocalDate.of(2026, 8, 3))
                .homepageUrl("https://k-startup.go.kr/1")
                .qualification("누구나")
                .sourceType("KSTARTUP")
                .externalId("178584")
                .build();

        boolean changed = postingMapper.applyChanges(existing,
                data("서울", "누구나", LocalDate.of(2026, 8, 3)));

        assertThat(changed).isFalse();
        assertThat(existing.getTitle()).isEqualTo("제목");
    }

    @Test
    void 마감일이_바뀌면_변경으로_감지하고_필드를_갱신한다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));
        Posting existing = Posting.builder()
                .categoryCode(governmentSupportCode)
                .regionCode(seoulCode)
                .title("제목")
                .company("회사")
                .content("내용")
                .endDate(LocalDate.of(2026, 8, 3))
                .homepageUrl("https://k-startup.go.kr/1")
                .qualification("누구나")
                .sourceType("KSTARTUP")
                .externalId("178584")
                .build();

        boolean changed = postingMapper.applyChanges(existing,
                data("서울", "누구나", LocalDate.of(2026, 9, 1)));

        assertThat(changed).isTrue();
        assertThat(existing.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void 변경이_없으면_카테고리_공통코드_조회조차_하지_않는다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));
        Posting existing = Posting.builder()
                .categoryCode(governmentSupportCode)
                .regionCode(seoulCode)
                .title("제목")
                .company("회사")
                .content("내용")
                .endDate(LocalDate.of(2026, 8, 3))
                .homepageUrl("https://k-startup.go.kr/1")
                .qualification("누구나")
                .sourceType("KSTARTUP")
                .externalId("178584")
                .build();

        postingMapper.applyChanges(existing, data("서울", "누구나", LocalDate.of(2026, 8, 3)));

        verify(codeRepository, never()).findByCodeTypeAndCode(any(), any());
    }
}
