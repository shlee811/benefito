package com.lion.benefito.domain.code.repository;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepository extends JpaRepository<Code, Long> {

    List<Code> findByCodeTypeAndIsActiveTrueOrderByDisplayOrderAsc(CommonCodeType codeType);

    Optional<Code> findByCode(String code);

    // 고정 카테고리(예: 정부지원사업)처럼 내부 코드값(Code.code)이 이미 알려진 경우의 조회.
    Optional<Code> findByCodeTypeAndCode(CommonCodeType codeType, String code);

    // 외부 API가 내려주는 지역명은 Code.code("SEOUL")이 아니라 Code.name("서울")과 일치하므로 name으로 조회한다.
    Optional<Code> findByCodeTypeAndName(CommonCodeType codeType, String name);
}
