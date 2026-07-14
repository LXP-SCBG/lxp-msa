package com.ohgiraffers.memberservice.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청.
 *
 * <p>여기의 형식 검증(@Valid)이 1차 방어선이다. 메시지는 프론트가 입력창 아래에 그대로 표시한다.
 * 길이·정규식 규칙은 프론트 검증과 동일하게 맞춰야 한다.
 * 비밀번호 확인값(passwordConfirm)은 프론트에서만 비교하고 서버로 보내지 않는다.
 */
public record SignupRequest(

        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자로 입력해 주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용할 수 있습니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상으로 입력해 주세요.")
        String password,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(min = 1, max = 20, message = "닉네임은 20자 이하로 입력해 주세요.")
        String nickname
) {
}
