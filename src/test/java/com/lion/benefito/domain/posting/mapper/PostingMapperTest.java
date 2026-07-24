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
import com.lion.benefito.domain.posting.entity.PostingStatus;
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
                "https://k-startup.go.kr/1", "사업화", region, qualification, null, deadline);
    }

    private ExternalPostingData publicInstitutionJobData(String region, LocalDate deadline) {
        return new ExternalPostingData("PUBLIC_INSTITUTION_JOB", "302908", "제목", "내용", "기관",
                "https://example.com/1", null, region, "학력무관", null, deadline);
    }

    private ExternalPostingData qNetData(LocalDate startDate, LocalDate deadline) {
        return new ExternalPostingData("QNET", "1320-2026년 정기 기사 1회-필기", "2026년 정보처리기사 제1회 필기시험",
                "필기시험 원서접수: 20260112 ~ 20260115", "한국산업인력공단(Q-Net)",
                "https://www.q-net.or.kr", null, null, "정보통신 > 정보기술", startDate, deadline);
    }

    private ExternalPostingData seedQNetScheduleData(LocalDate startDate, LocalDate deadline) {
        return new ExternalPostingData("SEED_QNET_SCHEDULE", "정보처리기사-2026-3회-필기",
                "2026년 정보처리기사 제3회 필기시험", "종목명: 정보처리기사", "한국산업인력공단(Q-Net)",
                "https://www.q-net.or.kr", null, "전국", "필기", startDate, deadline);
    }

    private ExternalPostingData scholarshipData(LocalDate startDate, LocalDate deadline) {
        return new ExternalPostingData("SCHOLARSHIP", "1", "테스트재단 생활비 장학금",
                "1인당 100만원 생활비 지원", "테스트재단",
                "https://example.com/1", null, null, "성적 3.0 이상", startDate, deadline);
    }

    private ExternalPostingData seedData(String externalId, String rawCategory) {
        return new ExternalPostingData("SEED", externalId, "테스트 공모전", "[시연용 샘플 데이터] 설명",
                "테스트재단", null, rawCategory, null, "전국 대학생", null, null);
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
    void SCHOLARSHIP_소스는_카테고리가_SCHOLARSHIP으로_고정된다() {
        Code scholarshipCode = mock(Code.class);
        when(scholarshipCode.getId()).thenReturn(500L);
        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "SCHOLARSHIP"))
                .thenReturn(Optional.of(scholarshipCode));

        Posting posting = postingMapper.toNewPosting(
                scholarshipData(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        assertThat(posting.getCategoryCode()).isEqualTo(scholarshipCode);
        assertThat(posting.getSourceType()).isEqualTo("SCHOLARSHIP");
        assertThat(posting.getRegionCode()).isNull();
        assertThat(posting.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void SEED_소스는_CONTEST_카테고리로_직접_조회된다() {
        Code contestCode = mock(Code.class);
        when(contestCode.getId()).thenReturn(600L);
        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "CONTEST"))
                .thenReturn(Optional.of(contestCode));

        Posting posting = postingMapper.toNewPosting(seedData("SEED-CONTEST-001", "CONTEST"));

        assertThat(posting.getCategoryCode()).isEqualTo(contestCode);
        assertThat(posting.getSourceType()).isEqualTo("SEED");
        assertThat(posting.getExternalId()).isEqualTo("SEED-CONTEST-001");
    }

    @Test
    void SEED_소스는_EXTRACURRICULAR_카테고리로도_직접_조회된다() {
        Code extracurricularCode = mock(Code.class);
        when(extracurricularCode.getId()).thenReturn(601L);
        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "EXTRACURRICULAR"))
                .thenReturn(Optional.of(extracurricularCode));

        Posting posting = postingMapper.toNewPosting(seedData("SEED-ACTIVITY-001", "EXTRACURRICULAR"));

        assertThat(posting.getCategoryCode()).isEqualTo(extracurricularCode);
        assertThat(posting.getExternalId()).isEqualTo("SEED-ACTIVITY-001");
    }

    @Test
    void QNET_소스는_카테고리가_CERTIFICATE로_고정되고_regionCode는_항상_null이다() {
        Code certificateCode = mock(Code.class);
        when(certificateCode.getId()).thenReturn(400L);
        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "CERTIFICATE"))
                .thenReturn(Optional.of(certificateCode));

        Posting posting = postingMapper.toNewPosting(
                qNetData(LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 15)));

        assertThat(posting.getCategoryCode()).isEqualTo(certificateCode);
        assertThat(posting.getSourceType()).isEqualTo("QNET");
        assertThat(posting.getRegionCode()).isNull();
        assertThat(posting.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 12));
        assertThat(posting.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        verify(codeRepository, never()).findByCodeTypeAndName(any(), any());
    }

    @Test
    void SEED_QNET_SCHEDULE_소스는_카테고리가_CERTIFICATE로_고정된다() {
        Code certificateCode = mock(Code.class);
        when(certificateCode.getId()).thenReturn(401L);
        when(codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "CERTIFICATE"))
                .thenReturn(Optional.of(certificateCode));
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "전국"))
                .thenReturn(Optional.empty());

        Posting posting = postingMapper.toNewPosting(
                seedQNetScheduleData(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)));

        assertThat(posting.getCategoryCode()).isEqualTo(certificateCode);
        assertThat(posting.getSourceType()).isEqualTo("SEED_QNET_SCHEDULE");
        assertThat(posting.getStatus()).isEqualTo(PostingStatus.ACTIVE);
    }

    @Test
    void 변경된_필드가_없으면_updateFrom을_호출하지_않고_false를_반환한다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));
        Posting existing = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.of(2026, 8, 3)));

        boolean changed = postingMapper.applyChanges(existing,
                data("서울", "누구나", LocalDate.of(2026, 8, 3)));

        assertThat(changed).isFalse();
        assertThat(existing.getTitle()).isEqualTo("제목");
    }

    @Test
    void 마감일이_바뀌면_변경으로_감지하고_필드와_contentHash를_갱신한다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));
        Posting existing = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.of(2026, 8, 3)));
        String originalHash = existing.getContentHash();

        boolean changed = postingMapper.applyChanges(existing,
                data("서울", "누구나", LocalDate.of(2026, 9, 1)));

        assertThat(changed).isTrue();
        assertThat(existing.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(existing.getContentHash()).isNotNull().isNotEqualTo(originalHash);
    }

    @Test
    void 변경이_없으면_카테고리_공통코드를_다시_조회하지_않는다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));
        Posting existing = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.of(2026, 8, 3)));
        // toNewPosting()에서 이미 한 번 호출됐으므로, 이후 호출 횟수 기준을 다시 잡는다.
        org.mockito.Mockito.clearInvocations(codeRepository);

        postingMapper.applyChanges(existing, data("서울", "누구나", LocalDate.of(2026, 8, 3)));

        verify(codeRepository, never()).findByCodeTypeAndCode(any(), any());
        verify(codeRepository, never()).findByCodeTypeAndName(any(), any());
    }

    @Test
    void 신규_공고는_contentHash가_채워진다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));

        Posting posting = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.of(2026, 8, 3)));

        assertThat(posting.getContentHash()).isNotNull().isNotBlank();
    }

    @Test
    void 마감일이_미래이면_status는_ACTIVE다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));

        Posting posting = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.now().plusDays(1)));

        assertThat(posting.getStatus()).isEqualTo(PostingStatus.ACTIVE);
    }

    @Test
    void 마감일이_지났으면_status는_CLOSED다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));

        Posting posting = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.now().minusDays(1)));

        assertThat(posting.getStatus()).isEqualTo(PostingStatus.CLOSED);
    }

    @Test
    void 마감일이_없으면_status는_ACTIVE다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));

        Posting posting = postingMapper.toNewPosting(data("서울", "누구나", null));

        assertThat(posting.getStatus()).isEqualTo(PostingStatus.ACTIVE);
    }

    @Test
    void applyChanges로_마감일이_지나게_바뀌면_status가_CLOSED로_전환된다() {
        when(codeRepository.findByCodeTypeAndName(CommonCodeType.REGION, "서울"))
                .thenReturn(Optional.of(seoulCode));
        Posting existing = postingMapper.toNewPosting(data("서울", "누구나", LocalDate.now().plusDays(1)));

        postingMapper.applyChanges(existing, data("서울", "누구나", LocalDate.now().minusDays(1)));

        assertThat(existing.getStatus()).isEqualTo(PostingStatus.CLOSED);
    }
}
