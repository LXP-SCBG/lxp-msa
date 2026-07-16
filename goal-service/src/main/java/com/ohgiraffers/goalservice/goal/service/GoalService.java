package com.ohgiraffers.goalservice.goal.service;

import com.ohgiraffers.goalservice.common.exception.BusinessException;
import com.ohgiraffers.goalservice.common.exception.ErrorCode;
import com.ohgiraffers.goalservice.goal.domain.DetailGoal;
import com.ohgiraffers.goalservice.goal.domain.LearningGoal;
import com.ohgiraffers.goalservice.goal.dto.GoalCreateRequest;
import com.ohgiraffers.goalservice.goal.dto.GoalCreateResponse;
import com.ohgiraffers.goalservice.goal.dto.GoalResponse;
import com.ohgiraffers.goalservice.goal.dto.GoalUpdateRequest;
import com.ohgiraffers.goalservice.goal.repository.DetailGoalRepository;
import com.ohgiraffers.goalservice.goal.repository.LearningGoalRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;


@Service
@Transactional(readOnly = true)
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);

    private static final int MIN_DETAIL_GOALS = 1;
    private static final int MAX_DETAIL_GOALS = 20;
    private static final int EXPIRE_HOURS = 24;

    private final LearningGoalRepository learningGoalRepository;
    private final DetailGoalRepository detailGoalRepository;
    private final RestClient memberRestClient;

    public GoalService(
            LearningGoalRepository learningGoalRepository,
            DetailGoalRepository detailGoalRepository,
            RestClient memberRestClient
    ) {
        this.learningGoalRepository = learningGoalRepository;
        this.detailGoalRepository = detailGoalRepository;
        this.memberRestClient = memberRestClient;
    }

    @Transactional
    public GoalCreateResponse createGoal(Long memberId, GoalCreateRequest request) {
        log.info("학습목표 생성 요청 - memberId: {}, 세부목표 수: {}", memberId,
                request.detailGoals() == null ? 0 : request.detailGoals().size());
        validateMember(memberId);
        validateDetailGoalCount(request.detailGoals());

        LearningGoal learningGoal = learningGoalRepository.save(
                LearningGoal.create(memberId, request.title())
        );

        List<GoalCreateRequest.DetailGoalItem> items = request.detailGoals();
        List<DetailGoal> detailGoals = IntStream.range(0, items.size())
                .mapToObj(i -> DetailGoal.create(
                        learningGoal.getLearningGoalId(), items.get(i).content(), i + 1))  // 배열 순서로 sortOrder 자동 부여
                .toList();

        detailGoalRepository.saveAll(detailGoals);  // 묶어서 처리

        log.info("학습목표 생성 완료 - goalId: {}", learningGoal.getLearningGoalId());
        return GoalCreateResponse.from(learningGoal, detailGoals);
    }

    public List<GoalResponse> findTodayGoals(Long memberId) {
        log.info("오늘의 학습목표 조회 요청 - memberId: {}", memberId);
        validateMember(memberId);

        LocalDateTime threshold = LocalDateTime.now().minusHours(EXPIRE_HOURS);
        List<LearningGoal> goals =
                learningGoalRepository.findByMemberIdAndCreatedAtAfterOrderByCreatedAtAsc(memberId, threshold);
        if (goals.isEmpty()) {
            return List.of();
        }

        List<Long> goalIds = goals.stream()
                .map(LearningGoal::getLearningGoalId)
                .toList();
        Map<Long, List<DetailGoal>> detailsByGoalId =
                detailGoalRepository.findByLearningGoalIdInOrderBySortOrderAsc(goalIds).stream()
                        .collect(Collectors.groupingBy(DetailGoal::getLearningGoalId));

        return goals.stream()
                .map(goal -> GoalResponse.of(
                        goal, detailsByGoalId.getOrDefault(goal.getLearningGoalId(), List.of())))
                .toList();
    }

    @Transactional
    public GoalResponse updateGoal(Long memberId, Long goalId, GoalUpdateRequest request) {
        log.info("학습목표 수정 요청 - memberId: {}, goalId: {}", memberId, goalId);
        validateMember(memberId);
        validateDetailGoalCount(request.detailGoals());

        LearningGoal goal = findOwnedGoal(goalId, memberId);
        goal.updateTitle(request.title());

        List<DetailGoal> result = applyDetailGoalChanges(goalId, request.detailGoals());
        return GoalResponse.of(goal, result);
    }

    @Transactional
    public GoalResponse updateDetailGoalCompletion(
            Long memberId, Long goalId, Long detailGoalId, boolean completed) {
        log.info("세부목표 완료 상태 변경 요청 - memberId: {}, goalId: {}, detailGoalId: {}, completed: {}",
                memberId, goalId, detailGoalId, completed);
        validateMember(memberId);
        LearningGoal goal = findOwnedGoal(goalId, memberId);

        List<DetailGoal> details = detailGoalRepository.findByLearningGoalIdOrderBySortOrderAsc(goalId);
        DetailGoal target = details.stream()
                .filter(detailGoal -> detailGoal.getDetailGoalId().equals(detailGoalId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_DETAIL_NOT_FOUND));

        target.changeCompletion(completed);  // 변경감지로 저장됨

        return GoalResponse.of(goal, details);
    }

    @Transactional
    public void deleteGoal(Long memberId, Long goalId) {
        log.info("학습목표 삭제 요청 - memberId: {}, goalId: {}", memberId, goalId);
        validateMember(memberId);
        LearningGoal goal = findOwnedGoal(goalId, memberId);

        List<DetailGoal> details = detailGoalRepository.findByLearningGoalIdOrderBySortOrderAsc(goalId);
        detailGoalRepository.deleteAll(details);  // @SQLDelete → 세부목표도 soft delete
        learningGoalRepository.delete(goal);       // @SQLDelete → 학습목표 soft delete
        log.info("학습목표 삭제 완료 - goalId: {}", goalId);
    }

    /** 세부목표를 요청된 최종 상태(추가/수정/삭제)로 동기화하고, 노출 순서대로 정렬된 결과를 반환한다. */
    private List<DetailGoal> applyDetailGoalChanges(
            Long goalId, List<GoalUpdateRequest.DetailGoalItem> items) {
        Map<Long, DetailGoal> existingById =
                detailGoalRepository.findByLearningGoalIdOrderBySortOrderAsc(goalId).stream()
                        .collect(Collectors.toMap(DetailGoal::getDetailGoalId, Function.identity()));

        List<DetailGoal> result = new ArrayList<>();
        Set<Long> keptIds = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            GoalUpdateRequest.DetailGoalItem item = items.get(i);
            int sortOrder = i + 1;  // 배열 순서대로 노출 순서 재부여
            if (item.detailGoalId() == null) {
                result.add(DetailGoal.create(goalId, item.content(), sortOrder));  // 신규
            } else {
                DetailGoal existing = existingById.get(item.detailGoalId());
                if (existing == null) {
                    throw new BusinessException(ErrorCode.GOAL_DETAIL_NOT_FOUND);
                }
                existing.update(item.content(), sortOrder);  // 기존 수정 (completed 유지)
                keptIds.add(existing.getDetailGoalId());
                result.add(existing);
            }
        }

        List<DetailGoal> toDelete = existingById.values().stream()
                .filter(detailGoal -> !keptIds.contains(detailGoal.getDetailGoalId()))
                .toList();
        detailGoalRepository.deleteAll(toDelete);
        detailGoalRepository.saveAll(result);  // 신규는 insert, 기존은 변경감지로 update

        return result;
    }

    /** 목표를 조회하고 작성자 본인인지 확인한다. 없으면 GOAL_NOT_FOUND, 본인이 아니면 GOAL_ACCESS_DENIED. */
    private LearningGoal findOwnedGoal(Long goalId, Long memberId) {
        LearningGoal goal = learningGoalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOAL_NOT_FOUND));
        if (!goal.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.GOAL_ACCESS_DENIED);
        }
        return goal;
    }

    private void validateMember(Long memberId) {

        ResponseEntity<String> response = memberRestClient.get()
                .uri("/api/v1/members/{id}/active", memberId)
                .retrieve()
                .toEntity(String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private void validateDetailGoalCount(List<?> detailGoals) {
        if (detailGoals == null || detailGoals.size() < MIN_DETAIL_GOALS) {
            throw new BusinessException(ErrorCode.GOAL_DETAIL_REQUIRED);
        }
        if (detailGoals.size() > MAX_DETAIL_GOALS) {
            throw new BusinessException(ErrorCode.GOAL_DETAIL_LIMIT_EXCEEDED);
        }
    }
}
