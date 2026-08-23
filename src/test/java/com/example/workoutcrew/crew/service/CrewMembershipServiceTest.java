package com.example.workoutcrew.crew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.global.exception.ErrorCode;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CrewMembershipServiceTest extends ApiIntegrationSupport {

    @Autowired CrewMembershipService membershipService;

    @Test
    void 가입은_중복_비밀번호_정원을_검증하고_MEMBER로_저장한다() {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "가입자");
        Crew crew = crewRepository.save(Crew.create("가입크루", passwordEncoder.encode("crew12"), 2, 3));
        crewUserRepository.saveAndFlush(CrewUser.manager(owner, crew));

        assertThatThrownBy(() -> membershipService.join(crew.getId(), owner.getId(), "wrong"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_CREW_MEMBER));
        assertThatThrownBy(() -> membershipService.join(crew.getId(), member.getId(), "wrong"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREW_PASSWORD));
        membershipService.join(crew.getId(), member.getId(), "crew12");
        assertThat(crewUserRepository.findByCrewIdAndUserId(crew.getId(), member.getId())).isPresent();
    }

    @Test
    void MEMBER_탈퇴는_소속만_지우고_MANAGER_탈퇴는_크루를_지운다() {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "가입자");
        Crew crew = crewRepository.save(Crew.create("탈퇴크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(member, crew));
        membershipService.leave(crew.getId(), member.getId());
        assertThat(crewRepository.existsById(crew.getId())).isTrue();
        membershipService.leave(crew.getId(), owner.getId());
        assertThat(crewRepository.existsById(crew.getId())).isFalse();
    }
}
