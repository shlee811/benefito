package com.lion.benefito.domain.user.exception;

import com.lion.benefito.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "회원을 찾을 수 없습니다."),
    USER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "USER403_1", "회원 정보를 수정/삭제할 권한이 없습니다."),
    USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "USER409_1", "이미 사용 중인 아이디입니다."),
    USER_WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "USER401_1", "비밀번호가 일치하지 않습니다."),
    PASSWORD_ALREADY_USED(HttpStatus.BAD_REQUEST, "USER400_1", "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
