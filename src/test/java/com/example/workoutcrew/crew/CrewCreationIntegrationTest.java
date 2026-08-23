package com.example.workoutcrew.crew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.dto.CrewCreateRequest;
import com.example.workoutcrew.crew.service.CrewService;
import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CrewCreationIntegrationTest extends ApiIntegrationSupport {

    @Autowired CrewService crewService;

    @Test
    void 크루와_생성자_MANAGER가_원자적으로_생성된다() {
        User owner = saveUser("owner@example.com", "크루관리자");
        Long crewId = crewService.create(owner.getId(), new CrewCreateRequest("원자크루", "crew12", 5, 3));
        assertThat(crewRepository.findById(crewId)).isPresent();
        assertThat(crewUserRepository.findByCrewIdAndUserId(crewId, owner.getId()).orElseThrow().getRole())
                .isEqualTo(CrewRole.MANAGER);

        assertThatThrownBy(() -> crewService.create(owner.getId(),
                new CrewCreateRequest("원자크루", "crew12", 5, 3))).isInstanceOf(BusinessException.class);
        assertThat(crewRepository.count()).isEqualTo(1);
        assertThat(crewUserRepository.count()).isEqualTo(1);
    }
}
