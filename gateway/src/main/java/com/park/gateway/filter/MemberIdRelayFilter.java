package com.park.gateway.filter;

import com.park.gateway.auth.SessionKeys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 프록시되는 모든 요청에 대해:
 * <ol>
 *   <li>외부에서 위조해 보낸 X-Member-Id 헤더를 제거한다.</li>
 *   <li>게이트웨이 세션에 memberId가 있으면 X-Member-Id 헤더로 실어 보낸다.</li>
 *   <li>회원탈퇴(DELETE /members/me) 성공 시 세션을 무효화한다.</li>
 * </ol>
 */
@Component
public class MemberIdRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 위조 방지: 클라이언트가 보낸 헤더는 무조건 버린다
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(SessionKeys.MEMBER_ID_HEADER))
                .build();
        ServerWebExchange strippedExchange = exchange.mutate().request(stripped).build();

        return strippedExchange.getSession().flatMap(session -> {
            Long memberId = session.getAttribute(SessionKeys.MEMBER_ID);

            ServerWebExchange target = strippedExchange;
            if (memberId != null) {
                ServerHttpRequest withHeader = strippedExchange.getRequest().mutate()
                        .header(SessionKeys.MEMBER_ID_HEADER, String.valueOf(memberId))
                        .build();
                target = strippedExchange.mutate().request(withHeader).build();
            }

            // 회원탈퇴 성공 시 세션 무효화
            if (isWithdraw(strippedExchange)) {
                ServerWebExchange finalTarget = target;
                return chain.filter(finalTarget).then(Mono.defer(() -> {
                    HttpStatusCode status = finalTarget.getResponse().getStatusCode();
                    if (status != null && status.is2xxSuccessful()) {
                        return session.invalidate();
                    }
                    return Mono.empty();
                }));
            }

            return chain.filter(target);
        });
    }

    private boolean isWithdraw(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod() == HttpMethod.DELETE
                && "/members/me".equals(exchange.getRequest().getPath().value());
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
