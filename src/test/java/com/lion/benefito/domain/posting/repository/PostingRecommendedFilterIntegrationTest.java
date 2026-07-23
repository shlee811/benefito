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
    void filter가_all이면_상태나_마감일과_무관하게_조회된다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        posting("rec-all-far", LocalDate.now().plusDays(30), PostingStatus.ACTIVE, category);
        posting("rec-all-closed", LocalDate.now().minusDays(1), PostingStatus.CLOSED, category);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactlyInAnyOrder("rec-all-far", "rec-all-closed");
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

    private int indexOfTitleContaining(List<String> titles, String needle) {
        for (int i = 0; i < titles.size(); i++) {
            if (titles.get(i).contains(needle)) {
                return i;
            }
        }
        throw new IllegalStateException("찾는 제목이 결과에 없습니다: " + needle);
    }
}
