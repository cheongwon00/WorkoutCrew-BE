package com.example.workoutcrew.crew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.crew.dto.CrewUpdateRequest;
import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CrewManagementServiceTest extends ApiIntegrationSupport {

    @Autowired CrewManagementService managementService;

    @Test
    void MANAGER만_부분수정_위임_추방을_수행한다() {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "크루원");
        User outsider = saveUser("out@example.com", "외부인");
        Crew crew = crewRepository.save(Crew.create("관리크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(member, crew));

        managementService.update(crew.getId(), owner.getId(), new CrewUpdateRequest(null, null, 6, null));
        assertThat(crewRepository.findById(crew.getId()).orElseThrow().getMaxUsers()).isEqualTo(6);
        assertThatThrownBy(() -> managementService.update(crew.getId(), outsider.getId(),
                new CrewUpdateRequest("실패크루", null, null, null))).isInstanceOf(BusinessException.class);

        managementService.transferManager(crew.getId(), owner.getId(), member.getId());
        assertThat(crewUserRepository.findByCrewIdAndUserId(crew.getId(), member.getId()).orElseThrow().getRole())
                .isEqualTo(CrewRole.MANAGER);
        managementService.kick(crew.getId(), member.getId(), owner.getId());
        assertThat(crewUserRepository.findByCrewIdAndUserId(crew.getId(), owner.getId())).isEmpty();
    }

    @Test
    void 현재인원보다_작은_정원과_MANAGER_추방은_충돌한다() {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "크루원");
        User another = saveUser("another@example.com", "추가원");
        Crew crew = crewRepository.save(Crew.create("상태크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.save(CrewUser.member(member, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(another, crew));
        assertThatThrownBy(() -> managementService.update(crew.getId(), owner.getId(),
                new CrewUpdateRequest(null, null, 2, null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> managementService.kick(crew.getId(), owner.getId(), owner.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void MANAGER의_크루_삭제는_크루와_모든_소속을_정리한다() {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "크루원");
        Crew crew = crewRepository.save(Crew.create("삭제대상", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(member, crew));
        managementService.delete(crew.getId(), owner.getId());
        assertThat(crewRepository.existsById(crew.getId())).isFalse();
        assertThat(crewUserRepository.countByCrewId(crew.getId())).isZero();
    }
}
