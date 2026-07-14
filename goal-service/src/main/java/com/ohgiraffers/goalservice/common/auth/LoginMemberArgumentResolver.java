package com.ohgiraffers.goalservice.common.auth;

import com.ohgiraffers.goalservice.common.exception.BusinessException;
import com.ohgiraffers.goalservice.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link LoginMember} 가 붙은 파라미터에 세션의 memberId 를 주입한다.
 *
 * <p>세션이 없거나 로그인 정보가 없으면 예외를 던지므로,
 * 각 도메인 컨트롤러는 "로그인 여부"를 직접 검사할 필요가 없다.
 *
 * <p>주의: 여기서는 세션에 담긴 memberId 만 반환한다. 세션 도중 탈퇴한 회원까지 막아야 하는
 * 엔드포인트는 서비스에서 회원을 조회할 때 활성 상태(ACTIVE)인지 함께 확인한다.
 * (회원 탈퇴 시 세션을 무효화하면 대부분의 경우는 자연히 차단된다.)
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
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_REQUIRED);
        }
        Object memberId = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        if (memberId == null) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_REQUIRED);
        }
        return memberId;
    }
}
