package com.ohgiraffers.memberservice.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청. 아이디·비밀번호는 null/공백일 수 없다.
 */
public record LoginRequest(

        @NotBlank(message = "아이디를 입력해 주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
