package com.lion.benefito.global.security.jwt.dto;

import lombok.Builder;

@Builder
public record JwtDTO(
        String accessToken,
        String refreshToken
) {
}