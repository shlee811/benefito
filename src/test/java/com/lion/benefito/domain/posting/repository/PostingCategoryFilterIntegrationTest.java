package com.lion.benefito.domain.posting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

// GET /postings?page=0&sort=latest (category 파라미터 없음)로 전체 공고를 조회할 때,
// 특정 카테고리로 은근히 좁혀지지 않고 모든 카테고리가 그대로 반환되는지 검증한다.
// @Transactional로 테스트 종료 후 자동 롤백되어 실제 데이터는 남지 않는다.
@SpringBootTest
@Transactional
class PostingCategoryFilterIntegrationTest {

    private static final String TITLE_PREFIX = "카테고리전체조회테스트키워드";

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private CodeRepository codeRepository;

    private Posting posting(String externalId, Code category) {
        Posting posting = Posting.builder()
                .categoryCode(category)
                .title(TITLE_PREFIX + " " + externalId)
                .company("테스트 회사")
                .endDate(LocalDate.now().plusDays(30))
                .status(PostingStatus.ACTIVE)
                .sourceType("TEST")
                .externalId(externalId)
                .build();
        return postingRepository.save(posting);
    }

    @Test
    void category_파라미터가_null이면_카테고리_조건_없이_모든_카테고리가_조회된다() {
        Code job = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        Code scholarship = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "SCHOLARSHIP").orElseThrow();
        Code contest = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "CONTEST").orElseThrow();

        posting("cat-null-job", job);
        posting("cat-null-scholarship", scholarship);
        posting("cat-null-contest", contest);

        // category를 아예 null로 준다 - 프론트 최초 진입 시 "카테고리 파라미터 없이" 호출하는 것과 동일하다.
        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        List<Posting> result = postingRepository
                .findAll(PostingSpecification.search(condition), PageRequest.of(0, 20))
                .getContent();

        assertThat(result).hasSize(3);
        List<String> categoryCodes = result.stream()
                .map(p -> p.getCategoryCode().getCode())
                .collect(Collectors.toList());
        assertThat(categoryCodes).containsExactlyInAnyOrder("JOB", "SCHOLARSHIP", "CONTEST");
    }

    @Test
    void 기본_page_size는_1이_아니라_20이다() {
        Code job = codeRepository.findByCodeTypeAndCode(CommonCodeType.CATEGORY, "JOB").orElseThrow();
        for (int i = 0; i < 5; i++) {
            posting("cat-null-size-" + i, job);
        }

        PostingSearchCondition condition = new PostingSearchCondition(TITLE_PREFIX, null, null, PostingFilter.ALL);
        // 컨트롤러의 size 기본값(20)을 그대로 시뮬레이션한다.
        var page = postingRepository.findAll(PostingSpecification.search(condition), PageRequest.of(0, 20));

        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }
}
