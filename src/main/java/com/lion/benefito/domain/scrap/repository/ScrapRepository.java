package com.lion.benefito.domain.scrap.repository;

import com.lion.benefito.domain.scrap.entity.Scrap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByUserIdAndPostingId(Long userId, Long postingId);
}
