package com.ohgiraffers.memberservice.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인한 회원의 memberId 를 컨트롤러 파라미터로 주입받는 애너테이션.
 *
 * <p><b>팀 공통 규약</b> — 인증이 필요한 모든 엔드포인트(수강 신청, 질문 등록,
 * 후기 작성, 학습목표 등 등)는 컨트롤러 메서드 파라미터에 이 애너테이션을 붙이면 된다.
 * 세션이 없으면 {@link LoginMemberArgumentResolver} 가 401(UnauthorizedException)을 던진다.
 *
 * <pre>{@code
 * @PostMapping("/api/enrollments")
 * public ResponseEntity<Void> enroll(@LoginMember Long memberId,
 *                                    @RequestBody EnrollRequest request) {
 *     enrollmentService.enroll(memberId, request.lectureId());
 *     ...
 * }
 * }</pre>
 *
 * <p>파라미터 타입은 반드시 {@link Long} 이어야 한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
}
