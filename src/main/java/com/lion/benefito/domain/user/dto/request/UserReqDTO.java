package com.lion.benefito.domain.user.dto.request;

import lombok.Builder;

public class UserReqDTO {

    @Builder
    public record LoginReq(
            String loginId,
            String password
    ){
    }

}
