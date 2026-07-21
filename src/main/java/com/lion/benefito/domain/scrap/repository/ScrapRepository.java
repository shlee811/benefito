package com.lion.benefito.domain.scrap.repository;

import com.lion.benefito.domain.scrap.entity.Scrap;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByUserIdAndPostingId(Long userId, Long postingId);

    boolean existsByUserIdAndPostingId(Long userId, Long postingId);

    @Query("select s from Scrap s join fetch s.posting p join fetch p.categoryCode c " +
           "where s.user.id = :userId " +
           "and (:categoryCode is null or c.code = :categoryCode) " +
           "order by s.createdAt desc")
    List<Scrap> findMyScrapsWithCategoryFilter(@Param("userId") Long userId, @Param("categoryCode") String categoryCode);

    @Query("select s from Scrap s join fetch s.posting p join fetch p.categoryCode c " +
           "where s.user.id = :userId " +
           "and p.endDate between :startDate and :endDate " +
           "and (:categoryCode is null or c.code = :categoryCode) " +
           "order by p.endDate asc")
    List<Scrap> findMyScrapsByWeeklyDeadline(@Param("userId") Long userId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate, @Param("categoryCode") String categoryCode);

    // 내 전체 스크랩 수 카운트
    long countByUserId(Long userId);

    // 내 스크랩 중 특정 마감일 기간 내에 해당하는 스크랩 수 카운트
    long countByUserIdAndPostingEndDateBetween(Long userId, java.time.LocalDate startDate, java.time.LocalDate endDate);
}
