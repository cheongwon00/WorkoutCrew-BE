package com.example.workoutcrew.crew.service;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.crew.dto.CrewMemberResponse;
import com.example.workoutcrew.crew.repository.CrewRepository;
import com.example.workoutcrew.crew.repository.CrewUserRepository;
import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.global.exception.ErrorCode;
import com.example.workoutcrew.global.config.RetryConfig.LockRetryExecutor;
import com.example.workoutcrew.global.response.PageData;
import com.example.workoutcrew.user.domain.User;
import com.example.workoutcrew.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrewMembershipService {

    private final CrewRepository crewRepository;
    private final CrewUserRepository crewUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LockRetryExecutor retryExecutor;

    public CrewMembershipService(CrewRepository crewRepository, CrewUserRepository crewUserRepository,
                                 UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 LockRetryExecutor retryExecutor) {
        this.crewRepository = crewRepository;
        this.crewUserRepository = crewUserRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.retryExecutor = retryExecutor;
    }

    public void join(Long crewId, Long userId, String rawPassword) {
        retryExecutor.execute(() -> joinOnce(crewId, userId, rawPassword));
    }

    private void joinOnce(Long crewId, Long userId, String rawPassword) {
        Crew crew = crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        if (crewUserRepository.existsByCrewIdAndUserId(crewId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_CREW_MEMBER);
        }
        if (!passwordEncoder.matches(rawPassword, crew.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREW_PASSWORD);
        }
        if (crewUserRepository.countByCrewId(crewId) >= crew.getMaxUsers()) {
            throw new BusinessException(ErrorCode.CREW_FULL);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        crewUserRepository.save(CrewUser.member(user, crew));
    }

    @Transactional(readOnly = true)
    public PageData<CrewMemberResponse> listMembers(Long crewId, Long userId, Pageable pageable) {
        if (!crewRepository.existsById(crewId)) throw new BusinessException(ErrorCode.CREW_NOT_FOUND);
        if (!crewUserRepository.existsByCrewIdAndUserId(crewId, userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Page<CrewMemberResponse> result = crewUserRepository.findByCrewId(crewId, pageable)
                .map(CrewMemberResponse::from);
        return PageData.from(result);
    }

    public void leave(Long crewId, Long userId) {
        retryExecutor.execute(() -> leaveOnce(crewId, userId));
    }

    private void leaveOnce(Long crewId, Long userId) {
        Crew crew = crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        CrewUser membership = crewUserRepository.findByCrewIdAndUserIdForUpdate(crewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        if (membership.getRole() == CrewRole.MANAGER) {
            crewUserRepository.deleteAllByCrewId(crewId);
            crewRepository.deleteById(crew.getId());
        } else {
            crewUserRepository.delete(membership);
        }
    }
}
