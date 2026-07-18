package com.lion.benefito.domain.posting.repository;

import com.lion.benefito.domain.posting.dto.CalendarDeadlineCountResponse;
import com.lion.benefito.domain.posting.entity.Posting;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE postings SET embedding = CAST(:embedding AS vector) WHERE id = :postingId", nativeQuery = true)
    int updateEmbedding(@Param("postingId") Long postingId, @Param("embedding") String embedding);

    // <=>는 pgvector의 코사인 거리 연산자다 (<->는 L2/유클리드 거리이므로 사용하지 않는다). 값이 작을수록 유사하다.
    @Query(value = """
            SELECT p.id AS id, p.title AS title, p.content AS content,
                   c.code AS categoryCode, p.homepage_url AS sourceUrl,
                   (p.embedding <=> CAST(:queryVector AS vector)) AS distance
            FROM postings p
            JOIN common_codes c ON c.id = p.category_code_id
            WHERE p.embedding IS NOT NULL
            ORDER BY distance ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<PostingSimilarityProjection> findSimilarPostings(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK);
}
