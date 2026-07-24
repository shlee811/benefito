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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class PostingSpecification {

    // "미선택"을 뜻하는 값으로 취급할 문자열들. 프론트가 "전체"를 표현하는 방식이
    // 빈 문자열/공백/ALL/전체 중 무엇이든 실수로 넘어와도 필터 자체를 적용하지 않는다.
    private static final String ALL_VALUE_ALL = "ALL";
    private static final String ALL_VALUE_KOREAN = "전체";

    private PostingSpecification() {
    }

    public static Specification<Posting> search(PostingSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addKeywordPredicate(predicates, condition, root, cb);
            addRegionPredicate(predicates, condition.region(), root, cb);
            addCategoryPredicate(predicates, condition.category(), root, cb);

            // 마감된(CLOSED) 공고는 기본 목록에서 제외한다(정렬 기준과 무관하게 항상 적용).
            predicates.add(cb.equal(root.get("status"), PostingStatus.ACTIVE));

            // Posting.isRecommended()와 동일한 마감 임박 기준을 쿼리 조건으로도 표현한다. status=ACTIVE는
            // 위에서 이미 걸었으므로 여기서는 마감 임박 구간만 추가로 좁힌다.
            // 로그인 사용자의 관심 카테고리 기반 개인화 추천은 PostingService에서
            // searchByInterestCategories()와 별도로 조합한다(관심사가 없거나 비로그인이면 이 기준만 쓰인다).
            if (condition.filter() == PostingFilter.RECOMMENDED) {
                addRecommendedDeadlinePredicate(predicates, root, cb);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 로그인 사용자의 관심 카테고리와 일치하는 ACTIVE 공고만 조회한다(개인화 추천의 1순위 대상).
    // condition.category()도 반드시 함께 적용해야 한다 - 그렇지 않으면 예를 들어
    // "category=CERTIFICATE&filter=recommended" 요청인데 사용자 관심 카테고리가 JOB이면
    // JOB 공고가 그대로 섞여 나오는 버그가 생긴다(실제 재현 확인됨).
    public static Specification<Posting> searchByInterestCategories(
            PostingSearchCondition condition, List<Long> interestCategoryIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addKeywordPredicate(predicates, condition, root, cb);
            addRegionPredicate(predicates, condition.region(), root, cb);
            addCategoryPredicate(predicates, condition.category(), root, cb);
            predicates.add(cb.equal(root.get("status"), PostingStatus.ACTIVE));
            predicates.add(root.get("categoryCode").get("id").in(interestCategoryIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 캘린더의 "시작 공고" 조건. Posting에 실제 게시/시작일(publishedAt/startDate) 필드가 아직 없어
    // createdAt(생성일시)의 날짜 부분을 임시 기준으로 사용한다(PostingService.getCalendar 참고,
    // 정식 필드가 추가되면 이 부분만 교체하면 된다).
    // status가 null이면(기본값) ACTIVE/CLOSED 상태와 무관하게 "실제 게시된 이력"을 모두 포함한다.
    public static Specification<Posting> calendarStart(
            LocalDateTime rangeStart, LocalDateTime rangeEndExclusive, String category, String region,
            PostingStatus status, boolean recommendedOnly, List<Long> interestCategoryIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), rangeStart));
            predicates.add(cb.lessThan(root.get("createdAt"), rangeEndExclusive));
            addCategoryPredicate(predicates, category, root, cb);
            addRegionPredicate(predicates, region, root, cb);
            addStatusPredicate(predicates, status, root, cb);
            addRecommendedPredicate(predicates, recommendedOnly, interestCategoryIds, root, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 캘린더의 "마감 공고" 조건. deadline(endDate)이 range 안에 있는(=null이 아닌) 공고만 대상이 된다.
    // status가 null이면(기본값) ACTIVE/CLOSED 상태와 무관하게 모두 포함한다(과거 달력 이력 조회 지원).
    public static Specification<Posting> calendarDeadline(
            LocalDate rangeStart, LocalDate rangeEndExclusive, String category, String region,
            PostingStatus status, boolean recommendedOnly, List<Long> interestCategoryIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), rangeStart));
            predicates.add(cb.lessThan(root.get("endDate"), rangeEndExclusive));
            addCategoryPredicate(predicates, category, root, cb);
            addRegionPredicate(predicates, region, root, cb);
            addStatusPredicate(predicates, status, root, cb);
            addRecommendedPredicate(predicates, recommendedOnly, interestCategoryIds, root, cb);
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
            List<Predicate> predicates, String region, Root<Posting> root, CriteriaBuilder cb) {
        String normalized = normalizeFilterValue(region);
        if (normalized != null) {
            Join<Posting, Code> regionJoin = root.join("regionCode");
            predicates.add(cb.equal(regionJoin.get("codeType"), CommonCodeType.REGION));
            predicates.add(cb.equal(regionJoin.get("code"), normalized));
        }
    }

    private static void addCategoryPredicate(
            List<Predicate> predicates, String category, Root<Posting> root, CriteriaBuilder cb) {
        String normalized = normalizeFilterValue(category);
        if (normalized != null) {
            Join<Posting, Code> categoryJoin = root.join("categoryCode");
            predicates.add(cb.equal(categoryJoin.get("codeType"), CommonCodeType.CATEGORY));
            predicates.add(cb.equal(categoryJoin.get("code"), normalized));
        }
    }

    private static void addStatusPredicate(
            List<Predicate> predicates, PostingStatus status, Root<Posting> root, CriteriaBuilder cb) {
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
    }

    // Posting.isRecommended(List<Long>)와 동일한 기준: ACTIVE이면서, 관심 카테고리와 일치하거나
    // 마감이 임박(RECOMMENDED_DEADLINE_WITHIN_DAYS 이내)해야 한다. 관심 카테고리가 없으면(비로그인/
    // 관심사 미설정) 마감 임박 기준만 적용된다(기존 공통 추천 기준으로 fallback).
    private static void addRecommendedPredicate(
            List<Predicate> predicates, boolean recommendedOnly, List<Long> interestCategoryIds,
            Root<Posting> root, CriteriaBuilder cb) {
        if (!recommendedOnly) {
            return;
        }
        LocalDate today = LocalDate.now();
        Predicate deadlineImminent = cb.between(
                root.get("endDate"), today, today.plusDays(Posting.RECOMMENDED_DEADLINE_WITHIN_DAYS));
        Predicate matchOrImminent = deadlineImminent;
        if (interestCategoryIds != null && !interestCategoryIds.isEmpty()) {
            Predicate categoryMatch = root.get("categoryCode").get("id").in(interestCategoryIds);
            matchOrImminent = cb.or(categoryMatch, deadlineImminent);
        }
        predicates.add(cb.equal(root.get("status"), PostingStatus.ACTIVE));
        predicates.add(matchOrImminent);
    }

    // region=ALL/전체는 "전국" 코드와 우연히 일치할 수 있는 category와 달리(REGION에는 실제 code=ALL이
    // 있음) "지역 미선택"을 뜻하는 값으로 통일한다(사용자 결정: region=ALL도 카테고리처럼 전체 조회).
    public static String normalizeFilterValue(String value) {
        if (value == null || value.isBlank() || isAllValue(value)) {
            return null;
        }
        return value;
    }

    private static boolean isAllValue(String value) {
        String trimmed = value.strip();
        return trimmed.equalsIgnoreCase(ALL_VALUE_ALL) || trimmed.equals(ALL_VALUE_KOREAN);
    }

    private static void addRecommendedDeadlinePredicate(List<Predicate> predicates, Root<Posting> root, CriteriaBuilder cb) {
        LocalDate today = LocalDate.now();
        predicates.add(cb.between(root.get("endDate"), today,
                today.plusDays(Posting.RECOMMENDED_DEADLINE_WITHIN_DAYS)));
    }
}
