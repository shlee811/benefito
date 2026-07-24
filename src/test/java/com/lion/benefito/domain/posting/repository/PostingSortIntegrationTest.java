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
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

// sort=latest/popular/deadline이 요구사항대로 정렬되는지(보조 정렬 키, null 처리, CLOSED 제외 포함) 검증한다.
// @Transactional로 테스트 종료 후 자동 롤백되어 실제 데이터는 남지 않는다.
@SpringBootTest
@Transactional
class PostingSortIntegrationTest {

    private static final String TITLE_PREFIX = "정렬테스트키워드";

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private EntityManager entityManager;

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

    // BaseTimeEntity.createdAt/updatedAt은 @CreatedDate/@LastModifiedDate로 자동 관리되어 빌더로
    // 지정할 수 없다. 보조 정렬 키 테스트를 결정적으로 만들기 위해, 저장 후 네이티브 쿼리로
    // 값을 원하는 시점으로 강제로 맞춘다.
    private void forceCreatedAt(Long postingId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE postings SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", postingId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private void forceUpdatedAt(Long postingId, LocalDateTime updatedAt) {
        entityManager.createNativeQuery("UPDATE postings SET updated_at = :updatedAt WHERE id = :id")
                .setParameter("updatedAt", updatedAt)
                .setParameter("id", postingId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void sort가_latest면_updatedAt_내림차순으로_정렬된다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Posting older = posting("latest-older", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);
        Posting newer = posting("latest-newer", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);

        LocalDateTime base = LocalDateTime.now();
        // updatedAt을 createdAt과 반대로 설정해 정렬 기준이 실제로 updatedAt이지 createdAt이
        // 아님을 검증한다(older가 더 최근에 수정됨).
        forceCreatedAt(older.getId(), base.minusMinutes(20));
        forceCreatedAt(newer.getId(), base.minusMinutes(10));
        forceUpdatedAt(older.getId(), base);
        forceUpdatedAt(newer.getId(), base.minusMinutes(15));

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.LATEST.toSort())
                .stream().toList();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("latest-older", "latest-newer");
    }

    @Test
    void sort가_latest면_updatedAt이_같을때_createdAt_내림차순을_보조_정렬로_사용한다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Posting older = posting("latest-tie-updatedAt-older", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);
        Posting newer = posting("latest-tie-updatedAt-newer", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);

        LocalDateTime sameUpdatedAt = LocalDateTime.now();
        forceUpdatedAt(older.getId(), sameUpdatedAt);
        forceUpdatedAt(newer.getId(), sameUpdatedAt);
        forceCreatedAt(older.getId(), sameUpdatedAt.minusMinutes(10));
        forceCreatedAt(newer.getId(), sameUpdatedAt);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.LATEST.toSort())
                .stream().toList();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("latest-tie-updatedAt-newer", "latest-tie-updatedAt-older");
    }

    @Test
    void sort가_latest면_updatedAt과_createdAt이_모두_같을때_id_내림차순을_보조_정렬로_사용한다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Posting first = posting("latest-tie-first", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);
        Posting second = posting("latest-tie-second", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);

        LocalDateTime sameInstant = LocalDateTime.now();
        forceCreatedAt(first.getId(), sameInstant);
        forceCreatedAt(second.getId(), sameInstant);
        forceUpdatedAt(first.getId(), sameInstant);
        forceUpdatedAt(second.getId(), sameInstant);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.LATEST.toSort())
                .stream().toList();

        // 모든 시간 필드가 동일하면 id가 더 큰(나중에 생성된) second가 먼저 나와야 한다.
        List<Long> ids = result.stream().map(Posting::getId).toList();
        assertThat(ids).containsExactly(second.getId(), first.getId());
    }

    @Test
    void category가_SCHOLARSHIP이고_sort가_latest여도_ACTIVE_공고가_정상적으로_조회된다() {
        // 리포트된 "장학금 + 최신순 = 0건" 버그에 대한 회귀 테스트. category 필터와 LATEST 정렬이
        // 함께 적용된 실제 컨트롤러/서비스 경로(Specification + Pageable)로 재현한다.
        Code scholarship = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "SCHOLARSHIP").orElseThrow();
        posting("scholarship-latest-1", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, scholarship);
        posting("scholarship-latest-2", LocalDate.now().plusDays(20), PostingStatus.ACTIVE, scholarship);
        posting("scholarship-latest-closed", LocalDate.now().minusDays(1), PostingStatus.CLOSED, scholarship);

        PostingSearchCondition condition =
                new PostingSearchCondition(TITLE_PREFIX, null, "SCHOLARSHIP", PostingFilter.ALL);
        Page<Posting> page = postingRepository.findAll(
                PostingSpecification.search(condition),
                PageRequest.of(0, 20, PostingSort.LATEST.toSort()));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().stream().map(Posting::getExternalId).toList())
                .containsExactlyInAnyOrder("scholarship-latest-1", "scholarship-latest-2");
    }

    @Test
    void sort가_deadline이면_마감일_오름차순으로_정렬되고_null은_맨_뒤에_온다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        posting("deadline-far", LocalDate.now().plusDays(30), PostingStatus.ACTIVE, category);
        posting("deadline-soon", LocalDate.now().plusDays(3), PostingStatus.ACTIVE, category);
        posting("deadline-null", null, PostingStatus.ACTIVE, category);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.DEADLINE.toSort())
                .stream().toList();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("deadline-soon", "deadline-far", "deadline-null");
    }

    @Test
    void sort가_deadline이고_마감일이_같으면_updatedAt_내림차순을_보조_정렬로_사용한다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        LocalDate sameDeadline = LocalDate.now().plusDays(10);
        Posting older = posting("deadline-tie-older", sameDeadline, PostingStatus.ACTIVE, category);
        Posting newer = posting("deadline-tie-newer", sameDeadline, PostingStatus.ACTIVE, category);

        LocalDateTime base = LocalDateTime.now();
        forceUpdatedAt(older.getId(), base.minusMinutes(10));
        forceUpdatedAt(newer.getId(), base);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.DEADLINE.toSort())
                .stream().toList();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("deadline-tie-newer", "deadline-tie-older");
    }

    @Test
    void sort가_deadline이어도_CLOSED_공고는_기본_목록에서_제외된다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        posting("deadline-active", LocalDate.now().plusDays(3), PostingStatus.ACTIVE, category);
        posting("deadline-closed", LocalDate.now().plusDays(1), PostingStatus.CLOSED, category);

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.DEADLINE.toSort())
                .stream().toList();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("deadline-active");
    }

    @Test
    void sort가_popular이면_scrapCount_내림차순으로_정렬된다() {
        Code category = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Posting lowScrap = posting("popular-low", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);
        Posting highScrap = posting("popular-high", LocalDate.now().plusDays(10), PostingStatus.ACTIVE, category);
        // 조회수(viewCount)는 반대로 설정해, 정렬 기준이 실제로 scrapCount이지 viewCount가
        // 아님을 검증한다.
        postingRepository.incrementViewCount(lowScrap.getId());
        postingRepository.incrementViewCount(lowScrap.getId());
        postingRepository.incrementScrapCount(highScrap.getId());
        postingRepository.incrementScrapCount(highScrap.getId());
        postingRepository.incrementScrapCount(lowScrap.getId());
        entityManager.flush();
        entityManager.clear();

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PostingSort.POPULAR.toSort())
                .stream().toList();

        List<String> externalIds = result.stream().map(Posting::getExternalId).toList();
        assertThat(externalIds).containsExactly("popular-high", "popular-low");
    }

    @Test
    void sort_파라미터가_없으면_기본값은_latest다() {
        assertThat(PostingSort.from(null)).isEqualTo(PostingSort.LATEST);
        assertThat(PostingSort.from("")).isEqualTo(PostingSort.LATEST);
    }
}
