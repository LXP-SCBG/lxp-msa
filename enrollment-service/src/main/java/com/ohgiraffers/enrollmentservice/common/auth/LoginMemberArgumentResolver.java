package com.ohgiraffers.enrollmentservice.common.auth;

import com.ohgiraffers.enrollmentservice.common.exception.BusinessException;
import com.ohgiraffers.enrollmentservice.common.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link LoginMember} 가 붙은 파라미터에 게이트웨이가 넘겨준
 * X-Member-Id 헤더 값을 주입한다.
 *
 * <p>세션 검증은 게이트웨이 책임이다. 서비스는 헤더가 없으면
 * 비로그인으로 간주하고 401을 던진다.
 */
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginMember.class);
        boolean isLong = Long.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && isLong;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String memberId = webRequest.getHeader(AuthHeader.MEMBER_ID);
        if (memberId == null || memberId.isBlank()) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_REQUIRED);
        }
        try {
            return Long.valueOf(memberId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_REQUIRED);
        }
    }
}
