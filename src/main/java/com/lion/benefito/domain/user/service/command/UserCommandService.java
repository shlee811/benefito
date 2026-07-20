package com.lion.benefito.domain.user.service.command;

import com.lion.benefito.domain.user.dto.request.UserReqDTO;
import com.lion.benefito.domain.user.dto.response.UserResDTO;

public interface UserCommandService {

    UserResDTO.SignUpResult signUp(UserReqDTO.SignUp request);

    void softDeleteUser(Long userId);

    UserResDTO.MyProfile updateProfile(Long userId, UserReqDTO.UpdateProfile request);

    void updatePassword(Long userId, UserReqDTO.UpdatePassword request);
}
