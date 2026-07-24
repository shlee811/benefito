package com.lion.benefito.domain.user.service.query;

import com.lion.benefito.domain.user.dto.response.UserResDTO;

public interface UserQueryService {

    UserResDTO.MyProfile getMyProfile(Long userId);

    boolean checkDuplicateLoginId(String loginId);
}
