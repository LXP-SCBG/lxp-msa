package com.ohgiraffers.memberservice.member.controller;

import com.ohgiraffers.memberservice.member.dto.MemberResponse;
import com.ohgiraffers.memberservice.member.service.MemberService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간 내부 API.
 *
 * <p>다른 서비스(lecture, enrollment, goal)가 RestClient로 호출한다.
 * 게이트웨이 라우트에 /api/v1/** 를 등록하지 않으므로 외부에서는 접근할 수 없다.
 *
 * <ul>
 *   <li>GET /api/v1/members              강사 목록 조회</li>
 *   <li>GET /api/v1/members/{id}         회원 단건 조회 (상태 무관)</li>
 *   <li>GET /api/v1/members/{id}/active  활성 회원 조회 (탈퇴 회원이면 404성 에러)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/members")
public class InternalMemberController {

    /** 내부 API 로거. 서비스 간 호출 추적(디버깅)용이라 debug 레벨을 쓴다. */
    private static final Logger log = LoggerFactory.getLogger(InternalMemberController.class);

    private final MemberService memberService;

    public InternalMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 강사 목록 조회 (lecture-service가 강의 목록의 강사 정보 조합에 사용). */
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getInstructors() {
        // 내부 호출 추적: 어떤 조회가 얼마나 자주 오는지 debug로 남긴다
        log.debug("[internal] 강사 목록 조회");
        List<MemberResponse> responses = memberService.findInstructors().stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /** 회원 단건 조회 (상태 무관 — 탈퇴 회원 닉네임 표시 등에 사용). */
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        log.debug("[internal] 회원 단건 조회: memberId={}", id);
        return ResponseEntity.ok(MemberResponse.from(memberService.findMember(id)));
    }

    /** 활성 회원 조회 (enrollment·goal이 활성 회원 검증에 사용). */
    @GetMapping("/{id}/active")
    public ResponseEntity<MemberResponse> getActiveMember(@PathVariable Long id) {
        log.debug("[internal] 활성 회원 조회: memberId={}", id);
        return ResponseEntity.ok(MemberResponse.from(memberService.findActiveMember(id)));
    }
}
