package com.example.workoutcrew.user.service;

import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.crew.repository.CrewRepository;
import com.example.workoutcrew.crew.repository.CrewUserRepository;
import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.global.exception.ErrorCode;
import com.example.workoutcrew.user.domain.User;
import com.example.workoutcrew.user.dto.SignUpRequest;
import com.example.workoutcrew.user.repository.UserRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final CrewUserRepository crewUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository, CrewRepository crewRepository,
                       CrewUserRepository crewUserRepository, PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.crewRepository = crewRepository;
        this.crewUserRepository = crewUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Long signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        if (userRepository.existsByNickname(request.nickname())) throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        User user = User.create(request.email(), passwordEncoder.encode(request.password()), request.nickname());
        return userRepository.save(user).getId();
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getNickname().equals(nickname)) return;
        if (userRepository.existsByNickname(nickname)) throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        user.changeNickname(nickname);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        List<Long> managedCrewIds = crewUserRepository
                .findByUserIdAndRoleOrderByCrewIdAsc(userId, CrewRole.MANAGER).stream()
                .map(CrewUser::getCrew)
                .map(crew -> crew.getId())
                .toList();
        if (!managedCrewIds.isEmpty()) {
            crewRepository.findAllByIdForUpdate(managedCrewIds);
            for (Long crewId : managedCrewIds) {
                crewUserRepository.deleteAllByCrewId(crewId);
                crewRepository.deleteById(crewId);
            }
        }
        crewUserRepository.deleteAllByUserId(userId);
        userRepository.deleteById(user.getId());
        userRepository.flush();
        eventPublisher.publishEvent(new UserWithdrawalCommittedEvent(userId));
    }
}
