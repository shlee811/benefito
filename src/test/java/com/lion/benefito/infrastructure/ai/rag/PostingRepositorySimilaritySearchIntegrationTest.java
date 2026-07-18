package com.lion.benefito.infrastructure.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.domain.posting.repository.PostingSimilarityProjection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 로컬 PostgreSQL(pgvector)에 연결해 <=> 코사인 거리 검색을 검증하는 통합 테스트.
 * "서로 다른 질문" 케이스 하나만 실제 OpenAI Embedding API를 호출하고, 나머지는 DB에 이미 저장된
 * embedding 값을 그대로 재사용해 API 호출 없이 SQL 동작만 검증한다.
 */
@SpringBootTest
class PostingRepositorySimilaritySearchIntegrationTest {

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private PostingRetriever postingRetriever;

    @Test
    void 자기_자신의_임베딩으로_검색하면_1위이자_topK개만_반환된다() {
        Posting source = postingRepository.findAll().stream()
                .filter(p -> p.getEmbedding() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("embedding이 있는 테스트 데이터가 없습니다."));

        List<PostingSimilarityProjection> results = postingRepository.findSimilarPostings(source.getEmbedding(), 3);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(source.getId());
        assertThat(results.get(0).getDistance()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-6));

        // <=> 결과가 오름차순(ASC)인지 확인
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getDistance()).isLessThanOrEqualTo(results.get(i + 1).getDistance());
        }
    }

    // updateEmbedding()은 @Modifying 쿼리라 트랜잭션이 필요하다. @Transactional을 붙이면
    // 테스트 종료 후 Spring이 자동으로 롤백해주므로 별도 복구 코드 없이 원래 DB 상태가 유지된다.
    @Test
    @Transactional
    void embedding이_null인_공고는_검색결과에서_제외된다() {
        Posting target = postingRepository.findAll().stream()
                .filter(p -> p.getEmbedding() != null)
                .skip(1)
                .findFirst()
                .orElseThrow();
        String originalEmbedding = target.getEmbedding();

        postingRepository.updateEmbedding(target.getId(), null);

        List<PostingSimilarityProjection> results = postingRepository.findSimilarPostings(originalEmbedding, 100);

        assertThat(results).noneMatch(r -> r.getId().equals(target.getId()));
    }

    @Test
    void 서로_다른_질문은_서로_다른_1위_결과를_반환한다() {
        List<PostingSimilarityResponse> aiResults =
                postingRetriever.findSimilarPostings("AI 딥러닝 컴퓨터비전 채용", 3);
        List<PostingSimilarityResponse> scholarshipResults =
                postingRetriever.findSimilarPostings("이공계 여학생 장학금 지원", 3);

        assertThat(aiResults).isNotEmpty();
        assertThat(scholarshipResults).isNotEmpty();
        assertThat(aiResults.get(0).postingId()).isNotEqualTo(scholarshipResults.get(0).postingId());
    }
}
