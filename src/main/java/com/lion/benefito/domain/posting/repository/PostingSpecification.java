package com.lion.benefito.domain.posting.repository;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.entity.PostingFilter;
import com.lion.benefito.domain.posting.entity.PostingStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class PostingSpecification {

    private PostingSpecification() {
    }

    public static Specification<Posting> search(PostingSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addKeywordPredicate(predicates, condition, root, cb);
            addRegionPredicate(predicates, condition, root, cb);
            addCategoryPredicate(predicates, condition, root, cb);

            // Posting.isRecommended()와 동일한 기준(ACTIVE + 마감 임박)을 쿼리 조건으로도 표현한다.
            // 로그인 사용자의 관심 카테고리 기반 개인화 추천은 PostingService에서
            // searchByInterestCategories()와 별도로 조합한다(관심사가 없거나 비로그인이면 이 기준만 쓰인다).
            if (condition.filter() == PostingFilter.RECOMMENDED) {
                addRecommendedDeadlinePredicate(predicates, root, cb);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 로그인 사용자의 관심 카테고리와 일치하는 ACTIVE 공고만 조회한다(개인화 추천의 1순위 대상).
    public static Specification<Posting> searchByInterestCategories(
            PostingSearchCondition condition, List<Long> interestCategoryIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addKeywordPredicate(predicates, condition, root, cb);
            addRegionPredicate(predicates, condition, root, cb);
            predicates.add(cb.equal(root.get("status"), PostingStatus.ACTIVE));
            predicates.add(root.get("categoryCode").get("id").in(interestCategoryIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addKeywordPredicate(
            List<Predicate> predicates, PostingSearchCondition condition, Root<Posting> root, CriteriaBuilder cb) {
        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            String likePattern = "%" + condition.keyword().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), likePattern),
                    cb.like(cb.lower(root.get("company")), likePattern)
            ));
        }
    }

    private static void addRegionPredicate(
            List<Predicate> predicates, PostingSearchCondition condition, Root<Posting> root, CriteriaBuilder cb) {
        if (condition.region() != null && !condition.region().isBlank()) {
            Join<Posting, Code> regionJoin = root.join("regionCode");
            predicates.add(cb.equal(regionJoin.get("codeType"), CommonCodeType.REGION));
            predicates.add(cb.equal(regionJoin.get("code"), condition.region()));
        }
    }

    private static void addCategoryPredicate(
            List<Predicate> predicates, PostingSearchCondition condition, Root<Posting> root, CriteriaBuilder cb) {
        if (condition.category() != null && !condition.category().isBlank()) {
            Join<Posting, Code> categoryJoin = root.join("categoryCode");
            predicates.add(cb.equal(categoryJoin.get("codeType"), CommonCodeType.CATEGORY));
            predicates.add(cb.equal(categoryJoin.get("code"), condition.category()));
        }
    }

    private static void addRecommendedDeadlinePredicate(List<Predicate> predicates, Root<Posting> root, CriteriaBuilder cb) {
        LocalDate today = LocalDate.now();
        predicates.add(cb.equal(root.get("status"), PostingStatus.ACTIVE));
        predicates.add(cb.between(root.get("endDate"), today,
                today.plusDays(Posting.RECOMMENDED_DEADLINE_WITHIN_DAYS)));
    }
}
