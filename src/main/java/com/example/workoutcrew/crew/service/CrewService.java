package com.example.workoutcrew.crew.service;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.crew.dto.CrewCreateRequest;
import com.example.workoutcrew.crew.dto.CrewSummaryResponse;
import com.example.workoutcrew.crew.repository.CrewRepository;
import com.example.workoutcrew.crew.repository.CrewUserRepository;
import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.global.exception.ErrorCode;
import com.example.workoutcrew.global.response.PageData;
import com.example.workoutcrew.user.domain.User;
import com.example.workoutcrew.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrewService {

    private final CrewRepository crewRepository;
    private final CrewUserRepository crewUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CrewService(CrewRepository crewRepository, CrewUserRepository crewUserRepository,
                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.crewRepository = crewRepository;
        this.crewUserRepository = crewUserRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Long create(Long userId, CrewCreateRequest request) {
        if (crewRepository.existsByName(request.name())) throw new BusinessException(ErrorCode.CREW_NAME_DUPLICATED);
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Crew crew = Crew.create(request.name(), passwordEncoder.encode(request.password()), request.maxUsers(),
                request.weeklyCertificationGoal());
        crewRepository.save(crew);
        crewUserRepository.save(CrewUser.manager(creator, crew));
        return crew.getId();
    }

    @Transactional(readOnly = true)
    public PageData<CrewSummaryResponse> list(Pageable pageable) {
        Page<CrewSummaryResponse> result = crewRepository.findAll(pageable)
                .map(crew -> CrewSummaryResponse.of(crew, crewUserRepository.countByCrewId(crew.getId())));
        return PageData.from(result);
    }
}
