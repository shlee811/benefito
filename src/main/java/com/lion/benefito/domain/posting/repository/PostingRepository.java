package com.lion.benefito.domain.posting.repository;

import com.lion.benefito.domain.posting.dto.CalendarDeadlineCountResponse;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostingRepository extends JpaRepository<Posting, Long>, JpaSpecificationExecutor<Posting> {

    @Query("""
            SELECT new com.lion.benefito.domain.posting.dto.CalendarDeadlineCountResponse(p.endDate, COUNT(p))
            FROM Posting p
            WHERE p.endDate >= :startDate AND p.endDate < :endDate
            GROUP BY p.endDate
            ORDER BY p.endDate ASC
            """)
    List<CalendarDeadlineCountResponse> countDeadlinesByEndDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Posting> findByEndDate(LocalDate endDate, Sort sort);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Posting p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postingId")
    int incrementViewCount(@Param("postingId") Long postingId);

    List<Posting> findByEmbeddingIsNull();

    // 외부 수집 공고의 중복 확인(sourceType + externalId)에 사용한다.
    Optional<Posting> findBySourceTypeAndExternalId(String sourceType, String externalId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE postings SET embedding = CAST(:embedding AS vector) WHERE id = :postingId", nativeQuery = true)
    int updateEmbedding(@Param("postingId") Long postingId, @Param("embedding") String embedding);

    // <=>는 pgvector의 코사인 거리 연산자다 (<->는 L2/유클리드 거리이므로 사용하지 않는다). 값이 작을수록 유사하다.
    // homepage_url이 example.com인 행은 데모용으로 수동 삽입된 시드 공고라 RAG 검색에서 제외한다.
    // homepage_url이 NULL인 정상 공고(예: SEED 소스는 원문 URL이 없음)까지 걸리지 않도록 NULL은 통과시킨다.
    @Query(value = """
            SELECT p.id AS id, p.title AS title, p.content AS content,
                   c.code AS categoryCode, p.homepage_url AS sourceUrl,
                   (p.embedding <=> CAST(:queryVector AS vector)) AS distance
            FROM postings p
            JOIN common_codes c ON c.id = p.category_code_id
            WHERE p.embedding IS NOT NULL
              AND (p.homepage_url IS NULL OR p.homepage_url NOT LIKE '%example.com%')
            ORDER BY distance ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<PostingSimilarityProjection> findSimilarPostings(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK);

    // 질문에서 카테고리가 명확히 감지된 경우, 해당 카테고리로 한정해 검색한다(엉뚱한 카테고리 추천 방지).
    @Query(value = """
            SELECT p.id AS id, p.title AS title, p.content AS content,
                   c.code AS categoryCode, p.homepage_url AS sourceUrl,
                   (p.embedding <=> CAST(:queryVector AS vector)) AS distance
            FROM postings p
            JOIN common_codes c ON c.id = p.category_code_id
            WHERE p.embedding IS NOT NULL
              AND (p.homepage_url IS NULL OR p.homepage_url NOT LIKE '%example.com%')
              AND c.code = :categoryCode
            ORDER BY distance ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<PostingSimilarityProjection> findSimilarPostingsByCategory(
            @Param("queryVector") String queryVector,
            @Param("categoryCode") String categoryCode,
            @Param("topK") int topK);

    // 이번 수집에서 조회되지 않은(=외부 소스 목록에서 사라진) 기존 ACTIVE 공고를 CLOSED로 전환한다.
    // 물리 삭제는 하지 않는다. externalIds가 비어 있으면(= 이번 수집 결과 자체가 없으면) 호출하지 않는다
    // (일시적 API 실패로 빈 목록이 온 경우까지 전부 닫아버리는 것을 막기 위해 호출 쪽에서 가드한다).
    // 호출하는 ExternalPostingIngestService는 의도적으로 클래스 단위 @Transactional을 두지 않으므로
    // (트랜잭션 설계 참고), @Modifying 쿼리가 활성 트랜잭션 없이 실행되지 않도록 여기서 직접 연다.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Posting p SET p.status = :closedStatus
            WHERE p.sourceType = :sourceType AND p.status = :activeStatus AND p.externalId NOT IN :externalIds
            """)
    int closeMissingFromSource(@Param("sourceType") String sourceType,
                                @Param("externalIds") List<String> externalIds,
                                @Param("activeStatus") PostingStatus activeStatus,
                                @Param("closedStatus") PostingStatus closedStatus);

    // 소스 구분 없이 마감일이 지난 ACTIVE 공고를 전부 CLOSED로 전환한다(일 1회 스케줄러에서 호출).
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Posting p SET p.status = :closedStatus
            WHERE p.status = :activeStatus AND p.endDate IS NOT NULL AND p.endDate < :today
            """)
    int closeExpiredPostings(@Param("today") LocalDate today,
                              @Param("activeStatus") PostingStatus activeStatus,
                              @Param("closedStatus") PostingStatus closedStatus);
}
