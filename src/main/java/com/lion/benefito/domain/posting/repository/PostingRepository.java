package com.lion.benefito.domain.posting.repository;

import com.lion.benefito.domain.posting.entity.Posting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<Posting, Long> {
}
