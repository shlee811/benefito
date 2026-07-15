package com.lion.benefito.domain.posting.repository;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.posting.dto.PostingSearchCondition;
import com.lion.benefito.domain.posting.entity.Posting;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class PostingSpecification {

    private PostingSpecification() {
    }

    public static Specification<Posting> search(PostingSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (condition.keyword() != null && !condition.keyword().isBlank()) {
                String likePattern = "%" + condition.keyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("company")), likePattern)
                ));
            }

            if (condition.region() != null && !condition.region().isBlank()) {
                Join<Posting, Code> regionJoin = root.join("regionCode");
                predicates.add(cb.equal(regionJoin.get("codeType"), CommonCodeType.REGION));
                predicates.add(cb.equal(regionJoin.get("code"), condition.region()));
            }

            if (condition.category() != null && !condition.category().isBlank()) {
                Join<Posting, Code> categoryJoin = root.join("categoryCode");
                predicates.add(cb.equal(categoryJoin.get("codeType"), CommonCodeType.CATEGORY));
                predicates.add(cb.equal(categoryJoin.get("code"), condition.category()));
            }

            // TODO: filter == RECOMMENDED 처리. ERD에 recommended 컬럼이 없어 추천 로직 미구현, 현재는 전체 조회와 동일하게 동작한다.

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
