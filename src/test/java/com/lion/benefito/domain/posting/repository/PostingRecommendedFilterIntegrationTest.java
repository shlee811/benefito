package com.lion.benefito.domain.posting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.dto.PostingPageResponse;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import com.lion.benefito.domain.posting.service.PostingService;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.entity.UserInterest;
import com.lion.benefito.domain.user.repository.UserInterestRepository;
import com.lion.benefito.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

// filter=recommended가 실제 DB/Specification 조합에서 Posting.isRecommended()와 같은 기준으로 동작하는지
// 검증한다. @Transactional로 테스트 종료 후 자동 롤백되어 실제 데이터는 남지 않는다.
// 기존 DB에 이미 수천 건이 있어 페이지네이션에 걸리지 않도록, 매 테스트 고유한 keyword로
// 이번에 만든 테스트 픽스처만 정확히 좁혀서 조회한다.
@SpringBootTest
@Transactional
class PostingRecommendedFilterIntegrationTest {

    private static final String TITLE_PREFIX = "추천필터테스트키워드";

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private PostingService postingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInterestRepository userInterestRepository;

    private Posting posting(String externalId, LocalDate endDate, PostingStatus status, Code category) {
        Posting posting = Posting.builder()
                .categoryCode(category)
                .title(TITLE_PREFIX + " " + externalId)
                .company("테스트 회사")
                .endDate(endDate)
                .status(status)
                .sourceType("TEST")
                .externalId(externalId)
                .build();
        return postingRepository.save(posting);
    }

    @Test
    void category_파라미터가_ALL이면_카테고리_조건_없이_전체_조회된다() {
        Code jobCategory = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code scholarshipCategory =
                codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "SCHOLARSHIP").orElseThrow();
        posting("category-all-job", LocalDate.now().plusDays(30), PostingStatus.ACTIVE, jobCategory);
        posting("category-all-scholarship", LocalDate.now().plusDays(30), PostingStatus.ACTIVE, scholarshipCategory);

        // "ALL"은 REGION 공통코드에는 있지만("전국") CATEGORY에는 없는 값이라, 고치기 전에는
        // 카테고리 조건이 0건으로 만들어 아래 두 건 모두 조회되지 않는 회귀 버그가 있었다.
        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, "ALL", PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactlyInAnyOrder("category-all-job", "category-all-scholarship");
    }

    @Test
    void region_파라미터가_ALL이거나_전체이면_지역_조건_없이_모든_지역이_조회된다() {
        Code jobCategory = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code nationwide = codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "ALL").orElseThrow();
        Code seoul = codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "SEOUL").orElseThrow();

        Posting nationwidePosting = Posting.builder()
                .categoryCode(jobCategory).regionCode(nationwide)
                .title(TITLE_PREFIX + " region-all-nationwide").company("테스트 회사")
                .endDate(LocalDate.now().plusDays(30)).status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("region-all-nationwide").build();
        postingRepository.save(nationwidePosting);

        Posting seoulPosting = Posting.builder()
                .categoryCode(jobCategory).regionCode(seoul)
                .title(TITLE_PREFIX + " region-all-seoul").company("테스트 회사")
                .endDate(LocalDate.now().plusDays(30)).status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("region-all-seoul").build();
        postingRepository.save(seoulPosting);

        // 사용자 결정: region=ALL/전체는 category=ALL과 동일하게 "지역 미선택"(전체 조회)을 뜻한다.
        // "전국" 코드와의 우연한 일치에 기대지 않고, 서울/경기 등 모든 지역이 함께 반환되어야 한다.
        PostingSearchCondition allCondition =
                new PostingSearchCondition(TITLE_PREFIX, "ALL", null, PostingFilter.ALL);
        List<String> allResult = postingRepository
                .findAll(PostingSpecification.search(allCondition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();
        assertThat(allResult).containsExactlyInAnyOrder("region-all-nationwide", "region-all-seoul");

        PostingSearchCondition koreanCondition =
                new PostingSearchCondition(TITLE_PREFIX, "전체", null, PostingFilter.ALL);
        List<String> koreanResult = postingRepository
                .findAll(PostingSpecification.search(koreanCondition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();
        assertThat(koreanResult).containsExactlyInAnyOrder("region-all-nationwide", "region-all-seoul");
    }

    @Test
    void region이_빈_문자열이거나_공백이면_지역_조건_없이_모든_지역이_조회된다() {
        Code jobCategory = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code seoul = codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "SEOUL").orElseThrow();
        Code busan = codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "BUSAN").orElseThrow();

        postingRepository.save(Posting.builder()
                .categoryCode(jobCategory).regionCode(seoul)
                .title(TITLE_PREFIX + " region-blank-seoul").company("테스트 회사")
                .endDate(LocalDate.now().plusDays(30)).status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("region-blank-seoul").build());
        postingRepository.save(Posting.builder()
                .categoryCode(jobCategory).regionCode(busan)
                .title(TITLE_PREFIX + " region-blank-busan").company("테스트 회사")
                .endDate(LocalDate.now().plusDays(30)).status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("region-blank-busan").build());

        PostingSearchCondition emptyCondition = new PostingSearchCondition(TITLE_PREFIX, "", null, PostingFilter.ALL);
        List<String> emptyResult = postingRepository
                .findAll(PostingSpecification.search(emptyCondition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();
        assertThat(emptyResult).containsExactlyInAnyOrder("region-blank-seoul", "region-blank-busan");

        PostingSearchCondition blankCondition =
                new PostingSearchCondition(TITLE_PREFIX, "   ", null, PostingFilter.ALL);
        List<String> blankResult = postingRepository
                .findAll(PostingSpecification.search(blankCondition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();
        assertThat(blankResult).containsExactlyInAnyOrder("region-blank-seoul", "region-blank-busan");
    }

    @Test
    void 실제_지역_코드가_전달되면_해당_지역으로만_필터링된다() {
        Code jobCategory = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code seoul = codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "SEOUL").orElseThrow();
        Code busan = codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "BUSAN").orElseThrow();

        postingRepository.save(Posting.builder()
                .categoryCode(jobCategory).regionCode(seoul)
                .title(TITLE_PREFIX + " region-specific-seoul").company("테스트 회사")
                .endDate(LocalDate.now().plusDays(30)).status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("region-specific-seoul").build());
        postingRepository.save(Posting.builder()
                .categoryCode(jobCategory).regionCode(busan)
                .title(TITLE_PREFIX + " region-specific-busan").company("테스트 회사")
                .endDate(LocalDate.now().plusDays(30)).status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("region-specific-busan").build());

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, "SEOUL", null, PostingFilter.ALL);
        List<String> result = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();

        assertThat(result).containsExactly("region-specific-seoul");
    }

    @Test
    void filter가_recommended면_ACTIVE이면서_마감_7일_이내인_공고만_조회된다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        posting("rec-soon", LocalDate.now().plusDays(3), PostingStatus.ACTIVE, category);
        posting("rec-far", LocalDate.now().plusDays(30), PostingStatus.ACTIVE, category);
        posting("rec-closed", LocalDate.now().plusDays(1), PostingStatus.CLOSED, category);

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.RECOMMENDED);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("rec-soon");
    }

    @Test
    void filter가_all이어도_마감일과_무관하게_조회되지만_CLOSED_공고는_기본_목록에서_제외된다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        posting("rec-all-far", LocalDate.now().plusDays(30), PostingStatus.ACTIVE, category);
        posting("rec-all-closed", LocalDate.now().minusDays(1), PostingStatus.CLOSED, category);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("rec-all-far");
    }

    @Test
    void 관심_카테고리를_등록한_로그인_사용자는_카테고리가_일치하는_ACTIVE_공고를_마감일과_무관하게_추천받는다() {
        Code jobCode = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code scholarshipCode = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "SCHOLARSHIP")
                .orElseThrow();

        // 관심사(JOB)와 일치하지만 마감이 한참 남은 공고 -> 개인화 기준으로는 추천되어야 한다.
        posting("interest-job-far", LocalDate.now().plusDays(60), PostingStatus.ACTIVE, jobCode);
        // 관심사와 무관하지만 마감이 임박한 공고 -> 기존 fallback 기준으로 추천되어야 한다.
        posting("fallback-scholarship-soon", LocalDate.now().plusDays(2), PostingStatus.ACTIVE, scholarshipCode);
        // 관심사와도 무관하고 마감도 안 임박한 공고 -> 추천되지 않아야 한다.
        posting("irrelevant-scholarship-far", LocalDate.now().plusDays(60), PostingStatus.ACTIVE, scholarshipCode);

        User user = userRepository.save(User.builder()
                .loginId("recIntegrationTestUser")
                .password("password")
                .name("추천통합테스트")
                .build());
        userInterestRepository.save(UserInterest.builder().user(user).categoryCode(jobCode).build());

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.RECOMMENDED);
        PostingPageResponse response = postingService.getPostings(condition, PageRequest.of(0, 100), user.getId());

        List<String> titles = response.content().stream().map(item -> item.title()).toList();
        assertThat(titles).anyMatch(title -> title.contains("interest-job-far"));
        assertThat(titles).anyMatch(title -> title.contains("fallback-scholarship-soon"));
        assertThat(titles).noneMatch(title -> title.contains("irrelevant-scholarship-far"));

        // 관심 카테고리 일치 항목이 fallback보다 먼저 나와야 한다("우선 추천 대상").
        int interestIndex = indexOfTitleContaining(titles, "interest-job-far");
        int fallbackIndex = indexOfTitleContaining(titles, "fallback-scholarship-soon");
        assertThat(interestIndex).isLessThan(fallbackIndex);

        boolean interestMatchIsRecommended = response.content().stream()
                .filter(item -> item.title().contains("interest-job-far"))
                .findFirst().orElseThrow().isRecommended();
        assertThat(interestMatchIsRecommended).isTrue();
    }

    @Test
    void 비로그인_사용자는_관심_카테고리와_무관하게_기존_마감임박_기준만_적용된다() {
        Code jobCode = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        posting("anon-job-far", LocalDate.now().plusDays(60), PostingStatus.ACTIVE, jobCode);
        posting("anon-job-soon", LocalDate.now().plusDays(2), PostingStatus.ACTIVE, jobCode);

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.RECOMMENDED);
        PostingPageResponse response = postingService.getPostings(condition, PageRequest.of(0, 100), null);

        List<String> titles = response.content().stream().map(item -> item.title()).toList();
        assertThat(titles).anyMatch(title -> title.contains("anon-job-soon"));
        assertThat(titles).noneMatch(title -> title.contains("anon-job-far"));
    }

    @Test
    void category_파라미터가_있으면_관심_카테고리와_달라도_추천_결과에_섞이지_않는다() {
        // 실제 재현된 버그: 로그인 사용자의 관심 카테고리가 JOB인데
        // category=CERTIFICATE&filter=recommended로 요청하면 JOB 공고가 섞여 나왔다.
        // searchByInterestCategories()가 condition.category()를 무시했던 것이 원인.
        Code jobCode = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code certificateCode = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "CERTIFICATE")
                .orElseThrow();

        posting("category-mix-job", LocalDate.now().plusDays(60), PostingStatus.ACTIVE, jobCode);
        posting("category-mix-certificate", LocalDate.now().plusDays(60), PostingStatus.ACTIVE, certificateCode);

        User jobInterestUser = userRepository.save(User.builder()
                .loginId("categoryMixUser").password("password").name("카테고리혼입테스트유저").build());
        userInterestRepository.save(UserInterest.builder().user(jobInterestUser).categoryCode(jobCode).build());

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, "CERTIFICATE", PostingFilter.RECOMMENDED);
        PostingPageResponse response = postingService.getPostings(condition, PageRequest.of(0, 100), jobInterestUser.getId());

        List<String> titles = response.content().stream().map(item -> item.title()).toList();
        assertThat(titles).noneMatch(title -> title.contains("category-mix-job"));
    }

    @Test
    void 관심사가_다른_두_사용자는_filter가_recommended일때_서로_다른_추천_결과를_받는다() {
        Code jobCode = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code scholarshipCode = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "SCHOLARSHIP")
                .orElseThrow();

        posting("diff-interest-job", LocalDate.now().plusDays(60), PostingStatus.ACTIVE, jobCode);
        posting("diff-interest-scholarship", LocalDate.now().plusDays(60), PostingStatus.ACTIVE, scholarshipCode);

        User jobUser = userRepository.save(User.builder()
                .loginId("diffInterestJobUser").password("password").name("JOB관심사용자").build());
        userInterestRepository.save(UserInterest.builder().user(jobUser).categoryCode(jobCode).build());

        User scholarshipUser = userRepository.save(User.builder()
                .loginId("diffInterestScholarshipUser").password("password").name("장학금관심사용자").build());
        userInterestRepository.save(
                UserInterest.builder().user(scholarshipUser).categoryCode(scholarshipCode).build());

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.RECOMMENDED);

        List<String> jobUserTitles = postingService
                .getPostings(condition, PageRequest.of(0, 100), jobUser.getId())
                .content().stream().map(item -> item.title()).toList();
        List<String> scholarshipUserTitles = postingService
                .getPostings(condition, PageRequest.of(0, 100), scholarshipUser.getId())
                .content().stream().map(item -> item.title()).toList();

        // 두 사용자 모두 마감이 임박하지 않아 fallback 기준으로는 추천되지 않으므로,
        // 관심 카테고리와 일치하는 공고만 정확히 추천 대상이 되어야 하고 그 결과는 서로 달라야 한다.
        assertThat(jobUserTitles).anyMatch(title -> title.contains("diff-interest-job"));
        assertThat(jobUserTitles).noneMatch(title -> title.contains("diff-interest-scholarship"));

        assertThat(scholarshipUserTitles).anyMatch(title -> title.contains("diff-interest-scholarship"));
        assertThat(scholarshipUserTitles).noneMatch(title -> title.contains("diff-interest-job"));

        assertThat(jobUserTitles).isNotEqualTo(scholarshipUserTitles);
    }

    private int indexOfTitleContaining(List<String> titles, String needle) {
        for (int i = 0; i < titles.size(); i++) {
            if (titles.get(i).contains(needle)) {
                return i;
            }
        }
        throw new IllegalStateException("찾는 제목이 결과에 없습니다: " + needle);
    }
}
