package com.example.workoutcrew.crew.service;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.crew.dto.CrewUpdateRequest;
import com.example.workoutcrew.crew.repository.CrewRepository;
import com.example.workoutcrew.crew.repository.CrewUserRepository;
import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.global.exception.ErrorCode;
import com.example.workoutcrew.global.config.RetryConfig.LockRetryExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CrewManagementService {

    private final CrewRepository crewRepository;
    private final CrewUserRepository crewUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final LockRetryExecutor retryExecutor;

    public CrewManagementService(CrewRepository crewRepository, CrewUserRepository crewUserRepository,
                                 PasswordEncoder passwordEncoder, LockRetryExecutor retryExecutor) {
        this.crewRepository = crewRepository;
        this.crewUserRepository = crewUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.retryExecutor = retryExecutor;
    }

    public void update(Long crewId, Long requesterId, CrewUpdateRequest request) {
        retryExecutor.execute(() -> updateOnce(crewId, requesterId, request));
    }

    private void updateOnce(Long crewId, Long requesterId, CrewUpdateRequest request) {
        Crew crew = lockCrew(crewId);
        requireManager(crewId, requesterId);
        if (request.name() != null && !request.name().equals(crew.getName())
                && crewRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.CREW_NAME_DUPLICATED);
        }
        long currentUsers = crewUserRepository.countByCrewId(crewId);
        String encodedPassword = request.password() == null ? null : passwordEncoder.encode(request.password());
        try {
            crew.update(request.name(), encodedPassword, request.maxUsers(),
                    request.weeklyCertificationGoal(), currentUsers);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.MAX_USERS_BELOW_CURRENT);
        }
    }

    public void delete(Long crewId, Long requesterId) {
        retryExecutor.execute(() -> deleteOnce(crewId, requesterId));
    }

    private void deleteOnce(Long crewId, Long requesterId) {
        Crew crew = lockCrew(crewId);
        requireManager(crewId, requesterId);
        crewUserRepository.deleteAllByCrewId(crewId);
        crewRepository.deleteById(crew.getId());
    }

    public void transferManager(Long crewId, Long requesterId, Long targetUserId) {
        retryExecutor.execute(() -> transferManagerOnce(crewId, requesterId, targetUserId));
    }

    private void transferManagerOnce(Long crewId, Long requesterId, Long targetUserId) {
        lockCrew(crewId);
        CrewUser currentManager = requireManager(crewId, requesterId);
        CrewUser target = crewUserRepository.findByCrewIdAndUserIdForUpdate(crewId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        if (requesterId.equals(targetUserId) || target.getRole() != CrewRole.MEMBER) {
            throw new BusinessException(ErrorCode.INVALID_CREW_STATE);
        }
        currentManager.demoteToMember();
        crewUserRepository.flush();
        target.promoteToManager();
        crewUserRepository.flush();
    }

    public void kick(Long crewId, Long requesterId, Long targetUserId) {
        retryExecutor.execute(() -> kickOnce(crewId, requesterId, targetUserId));
    }

    private void kickOnce(Long crewId, Long requesterId, Long targetUserId) {
        lockCrew(crewId);
        requireManager(crewId, requesterId);
        CrewUser target = crewUserRepository.findByCrewIdAndUserIdForUpdate(crewId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        if (target.getRole() != CrewRole.MEMBER) throw new BusinessException(ErrorCode.INVALID_CREW_STATE);
        crewUserRepository.delete(target);
    }

    private Crew lockCrew(Long crewId) {
        return crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
    }

    private CrewUser requireManager(Long crewId, Long userId) {
        CrewUser membership = crewUserRepository.findByCrewIdAndUserIdForUpdate(crewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
        if (membership.getRole() != CrewRole.MANAGER) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return membership;
    }
}
