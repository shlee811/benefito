package com.lion.benefito.domain.code.service;

import com.lion.benefito.domain.code.dto.CommonCodeResponse;
import com.lion.benefito.domain.code.entity.CommonCodeType;
import com.lion.benefito.domain.code.repository.CodeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeRepository codeRepository;

    public List<CommonCodeResponse> getCodesByType(CommonCodeType codeType) {
        return codeRepository.findByCodeTypeAndIsActiveTrueOrderByDisplayOrderAsc(codeType)
                .stream()
                .map(CommonCodeResponse::from)
                .toList();
    }
}
