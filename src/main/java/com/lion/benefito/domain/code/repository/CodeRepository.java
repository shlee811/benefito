package com.lion.benefito.domain.code.repository;

import com.lion.benefito.domain.code.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepository extends JpaRepository<Code, Long> {
}
