package com.park.gateway.auth;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 게이트웨이 인증 엔드포인트. 세션의 주인은 게이트웨이다.
 *
 * <p>흐름:
 * <ol>
 *   <li>로그인/회원가입 요청을 member-service로 중계해 검증한다.</li>
 *   <li>성공 응답(memberId 포함)을 받으면 게이트웨이 WebSession에 memberId를 저장한다.</li>
 *   <li>이후 모든 프록시 요청에는 MemberIdRelayFilter가 세션의 memberId를
 *       X-Member-Id 헤더로 실어 보낸다.</li>
 * </ol>
 *
 * <p>애너테이션 컨트롤러가 게이트웨이 라우트보다 우선 매칭되므로,
 * 여기 정의한 경로는 프록시되지 않고 게이트웨이가 직접 처리한다.
 */
@RestController
public class AuthController {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient memberWebClient;

    public AuthController(
            @Value("${services.member.url}") String memberServiceUrl
    ) {
        // WebClient.Builder 빈에 의존하지 않고 직접 생성 (게이트웨이엔 해당 빈이 없을 수 있음)
        this.memberWebClient = WebClient.create(memberServiceUrl);
    }

    /** 로그인: member-service 검증 성공 시 게이트웨이 세션 생성. */
    @PostMapping("/auth/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(
            @RequestBody Map<String, Object> request,
            ServerWebExchange exchange
    ) {
        return relayAndCreateSession("/auth/login", request, exchange);
    }

    /** 회원가입: 성공 시 세션을 만들어 자동 로그인 처리한다. */
    @PostMapping("/members")
    public Mono<ResponseEntity<Map<String, Object>>> signup(
            @RequestBody Map<String, Object> request,
            ServerWebExchange exchange
    ) {
        return relayAndCreateSession("/members", request, exchange);
    }

    /** 로그아웃: 게이트웨이 세션 무효화로 끝. member-service 호출 불필요. */
    @PostMapping("/auth/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> session.invalidate())
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    private Mono<ResponseEntity<Map<String, Object>>> relayAndCreateSession(
            String path,
            Map<String, Object> request,
            ServerWebExchange exchange
    ) {
        return memberWebClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response -> response.toEntity(MAP_TYPE))
                .flatMap(entity -> {
                    // 실패 응답(잘못된 비밀번호 등)은 그대로 클라이언트에 전달
                    if (!entity.getStatusCode().is2xxSuccessful() || entity.getBody() == null) {
                        return Mono.just(entity);
                    }
                    Object memberId = entity.getBody().get("memberId");
                    if (memberId == null) {
                        return Mono.just(entity);
                    }
                    return exchange.getSession().map(session -> {
                        session.getAttributes()
                                .put(SessionKeys.MEMBER_ID, ((Number) memberId).longValue());
                        return entity;
                    });
                });
    }
}
