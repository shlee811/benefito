package com.lion.benefito.domain.user.dto.request;

import com.lion.benefito.domain.user.enums.Grade;
import com.lion.benefito.domain.user.enums.Income;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class UserReqDTO {

    @Builder
    public record LoginReq(
            String loginId,
            String password
    ) {
    }

    @Builder
    public record SignUp(
            @NotBlank(message = "아이디는 필수 입력 값입니다.")
            String loginId,

            @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
            String password,

            @NotBlank(message = "이름은 필수 입력 값입니다.")
            String name,

            @NotBlank(message = "학교명은 필수 입력 값입니다.")
            String school,

            @NotBlank(message = "전공명은 필수 입력 값입니다.")
            String major,

            @NotNull(message = "생년월일은 필수 입력 값입니다.")
            LocalDate birthDate,

            @NotNull(message = "학년 정보는 필수 입력 값입니다.")
            Grade grade,

            @NotBlank(message = "거주 지역은 필수 입력 값입니다.")
            String regionCode,

            Income income,

            @Size(max = 5, message = "관심분야는 최대 5개까지만 선택할 수 있습니다.")
            List<String> categoryCodes
    ) {
    }

    @Builder
    public record UpdateProfile(
            @NotBlank(message = "이름은 필수 입력 값입니다.")
            String name,

            @NotBlank(message = "학교명은 필수 입력 값입니다.")
            String school,

            @NotBlank(message = "전공명은 필수 입력 값입니다.")
            String major,

            @NotNull(message = "생년월일은 필수 입력 값입니다.")
            LocalDate birthDate,

            @NotNull(message = "학년 정보는 필수 입력 값입니다.")
            Grade grade,

            @NotBlank(message = "거주 지역은 필수 입력 값입니다.")
            String regionCode,

            Income income
    ) {
    }

    @Builder
    public record UpdateInterests(
            @NotNull(message = "관심분야 리스트는 필수 입력 값입니다. (빈 리스트 가능)")
            @Size(max = 5, message = "관심분야는 최대 5개까지만 선택할 수 있습니다.")
            List<String> categoryCodes
    ) {
    }

    @Builder
    public record UpdatePassword(
            @NotBlank(message = "현재 비밀번호는 필수 입력 값입니다.")
            String oldPassword,

            @NotBlank(message = "새로운 비밀번호는 필수 입력 값입니다.")
            String newPassword
    ) {
    }
}
