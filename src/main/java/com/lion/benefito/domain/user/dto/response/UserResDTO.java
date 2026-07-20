package com.lion.benefito.domain.user.dto.response;

import com.lion.benefito.domain.user.enums.Grade;
import com.lion.benefito.domain.user.enums.Income;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class UserResDTO {

    @Builder
    public record SignUpResult(
            Long id,
            String loginId,
            LocalDateTime createdAt
    ) {
    }

    @Builder
    public record MyProfile(
            Long id,
            String loginId,
            String name,
            String school,
            String major,
            LocalDate birthDate,
            Grade grade,
            String regionName,
            Income income,
            List<String> categoryNames
    ) {
    }
}
