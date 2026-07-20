package com.lion.benefito.domain.code.repository;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepository extends JpaRepository<Code, Long> {

    List<Code> findByCodeTypeAndIsActiveTrueOrderByDisplayOrderAsc(CommonCodeType codeType);

    Optional<Code> findByCode(String code);
}
