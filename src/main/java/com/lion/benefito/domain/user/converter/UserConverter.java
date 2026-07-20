package com.lion.benefito.domain.user.converter;

import com.lion.benefito.domain.code.entity.Code;
import com.lion.benefito.domain.user.dto.request.UserReqDTO;
import com.lion.benefito.domain.user.dto.response.UserResDTO;
import com.lion.benefito.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserConverter {

    public static User toUser(UserReqDTO.SignUp request, String encodedPassword, Code regionCode) {
        return User.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .name(request.name())
                .school(request.school())
                .major(request.major())
                .birthDate(request.birthDate())
                .grade(request.grade())
                .regionCode(regionCode)
                .income(request.income())
                .build();
    }

    public static UserResDTO.SignUpResult toSignUpResult(User user) {
        return UserResDTO.SignUpResult.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static UserResDTO.MyProfile toMyProfile(User user, List<String> categoryNames) {
        String regionName = (user.getRegionCode() != null) ? user.getRegionCode().getName() : null;
        return UserResDTO.MyProfile.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .school(user.getSchool())
                .major(user.getMajor())
                .birthDate(user.getBirthDate())
                .grade(user.getGrade())
                .regionName(regionName)
                .income(user.getIncome())
                .categoryNames(categoryNames)
                .build();
    }
}
