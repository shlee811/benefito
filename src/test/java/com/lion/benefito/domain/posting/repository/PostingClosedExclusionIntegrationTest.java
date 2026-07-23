package com.lion.benefito.domain.posting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingSort;
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

// CLOSED 공고가 어떤 화면(전체/카테고리별/추천/검색/지역 필터/정렬)에서도 노출되지 않는지를
// 한 곳에 모아 검증하는 회귀 테스트. 각 테스트마다 같은 조건의 ACTIVE 공고와 CLOSED 공고를
// 함께 만들어, CLOSED가 결과에서 완전히 빠지는지 확인한다.
// @Transactional로 테스트 종료 후 자동 롤백되어 실제 데이터는 남지 않는다.
@SpringBootTest
@Transactional
class PostingClosedExclusionIntegrationTest {

    private static final String TITLE_PREFIX = "CLOSED제외검증키워드";

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

    private Code job() {
        return codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
    }

    private Code seoul() {
        return codeRepository.findByCodeTypeAndCode(CommonCodeType.REGION, "SEOUL").orElseThrow();
    }

    private Posting posting(String externalId, String titleSuffix, PostingStatus status, Code category, Code region) {
        Posting posting = Posting.builder()
                .categoryCode(category)
                .regionCode(region)
                .title(TITLE_PREFIX + " " + titleSuffix + " " + externalId)
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(10))
                .status(status)
                .sourceType("TEST")
                .externalId(externalId)
                .build();
        return postingRepository.save(posting);
    }

    @Test
    void 전체_공고_조회에서_CLOSED는_제외된다() {
        posting("closed-all-active", "테스트공고", PostingStatus.ACTIVE, job(), null);
        posting("closed-all-closed", "테스트공고", PostingStatus.CLOSED, job(), null);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<String> externalIds = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();

        assertThat(externalIds).containsExactly("closed-all-active");
    }

    @Test
    void 카테고리별_조회에서_CLOSED는_제외된다() {
        posting("closed-category-active", "카테고리", PostingStatus.ACTIVE, job(), null);
        posting("closed-category-closed", "카테고리", PostingStatus.CLOSED, job(), null);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, "JOB", PostingFilter.ALL);
        List<String> externalIds = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();

        assertThat(externalIds).containsExactly("closed-category-active");
    }

    @Test
    void 지역_필터_조회에서_CLOSED는_제외된다() {
        posting("closed-region-active", "지역", PostingStatus.ACTIVE, job(), seoul());
        posting("closed-region-closed", "지역", PostingStatus.CLOSED, job(), seoul());

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, "SEOUL", null, PostingFilter.ALL);
        List<String> externalIds = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();

        assertThat(externalIds).containsExactly("closed-region-active");
    }

    @Test
    void 키워드_검색_결과에서_CLOSED는_제외된다() {
        posting("closed-keyword-active", "검색용유니크제목", PostingStatus.ACTIVE, job(), null);
        posting("closed-keyword-closed", "검색용유니크제목", PostingStatus.CLOSED, job(), null);

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX + " 검색용유니크제목", null, null, PostingFilter.ALL);
        List<String> externalIds = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 100))
                .getContent().stream().map(Posting::getExternalId).toList();

        assertThat(externalIds).containsExactly("closed-keyword-active");
    }

    @Test
    void 최신순_정렬_결과에서_CLOSED는_제외된다() {
        posting("closed-latest-active", "정렬", PostingStatus.ACTIVE, job(), null);
        posting("closed-latest-closed", "정렬", PostingStatus.CLOSED, job(), null);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<String> externalIds = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.LATEST.toSort())
                .stream().map(Posting::getExternalId).toList();

        assertThat(externalIds).containsExactly("closed-latest-active");
    }

    @Test
    void 인기순_정렬_결과에서_CLOSED는_제외된다() {
        Posting active = posting("closed-popular-active", "정렬", PostingStatus.ACTIVE, job(), null);
        posting("closed-popular-closed", "정렬", PostingStatus.CLOSED, job(), null);
        postingRepository.incrementScrapCount(active.getId());

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<String> externalIds = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.POPULAR.toSort())
                .stream().map(Posting::getExternalId).toList();

        assertThat(externalIds).containsExactly("closed-popular-active");
    }

    @Test
    void 마감순_정렬_결과에서_CLOSED는_제외된다() {
        posting("closed-deadline-active", "정렬", PostingStatus.ACTIVE, job(), null);
        posting("closed-deadline-closed", "정렬", PostingStatus.CLOSED, job(), null);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<String> externalIds = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.DEADLINE.toSort())
                .stream().map(Posting::getExternalId).toList();

        assertThat(externalIds).containsExactly("closed-deadline-active");
    }

    @Test
    void 추천_공고_비로그인_조회에서_CLOSED는_제외된다() {
        // 마감 임박 기준(RECOMMENDED_DEADLINE_WITHIN_DAYS 이내)에 걸리도록 마감일을 가깝게 설정한다.
        Posting active = postingRepository.save(Posting.builder()
                .categoryCode(job())
                .title(TITLE_PREFIX + " 추천 closed-rec-active")
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(2))
                .status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("closed-rec-active").build());
        postingRepository.save(Posting.builder()
                .categoryCode(job())
                .title(TITLE_PREFIX + " 추천 closed-rec-closed")
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(2))
                .status(PostingStatus.CLOSED)
                .sourceType("TEST").externalId("closed-rec-closed").build());

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.RECOMMENDED);
        var response = postingService.getPostings(condition, PageRequest.of(0, 100), null);

        List<String> titles = response.content().stream().map(item -> item.title()).toList();
        assertThat(titles).anyMatch(title -> title.contains("closed-rec-active"));
        assertThat(titles).noneMatch(title -> title.contains("closed-rec-closed"));
    }

    @Test
    void 추천_공고_로그인_관심카테고리_조회에서_CLOSED는_제외된다() {
        Code jobCode = job();
        postingRepository.save(Posting.builder()
                .categoryCode(jobCode)
                .title(TITLE_PREFIX + " 추천관심 closed-rec-interest-active")
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(60))
                .status(PostingStatus.ACTIVE)
                .sourceType("TEST").externalId("closed-rec-interest-active").build());
        postingRepository.save(Posting.builder()
                .categoryCode(jobCode)
                .title(TITLE_PREFIX + " 추천관심 closed-rec-interest-closed")
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(60))
                .status(PostingStatus.CLOSED)
                .sourceType("TEST").externalId("closed-rec-interest-closed").build());

        User user = userRepository.save(User.builder()
                .loginId("closedExclusionUser").password("password").name("CLOSED제외테스트유저").build());
        userInterestRepository.save(UserInterest.builder().user(user).categoryCode(jobCode).build());

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.RECOMMENDED);
        var response = postingService.getPostings(condition, PageRequest.of(0, 100), user.getId());

        List<String> titles = response.content().stream().map(item -> item.title()).toList();
        assertThat(titles).anyMatch(title -> title.contains("closed-rec-interest-active"));
        assertThat(titles).noneMatch(title -> title.contains("closed-rec-interest-closed"));
    }
}
