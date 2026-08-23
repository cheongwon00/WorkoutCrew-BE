package com.example.workoutcrew.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.workoutcrew.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserWithdrawalIntegrationTest extends ApiIntegrationSupport {

    @Autowired UserService userService;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void 탈퇴하면_관리_크루와_일반_소속과_모든_세션이_정리된다() throws Exception {
        User target = saveUser("withdraw@example.com", "탈퇴사용자");
        User other = saveUser("manager@example.com", "다른관리자");
        Crew managed = crewRepository.save(Crew.create("삭제크루", passwordEncoder.encode("crew12"), 5, 3));
        Crew joined = crewRepository.save(Crew.create("잔존크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(target, managed));
        crewUserRepository.save(CrewUser.manager(other, joined));
        crewUserRepository.saveAndFlush(CrewUser.member(target, joined));

        MockHttpSession sessionA = new MockHttpSession();
        MockHttpSession sessionB = new MockHttpSession();
        login(sessionA);
        login(sessionB);
        mockMvc.perform(delete("/api/v1/users/me").session(sessionA).with(csrf())).andExpect(status().isOk());

        assertThat(userRepository.existsById(target.getId())).isFalse();
        assertThat(crewRepository.existsById(managed.getId())).isFalse();
        assertThat(crewRepository.existsById(joined.getId())).isTrue();
        assertThat(crewUserRepository.findByUserId(target.getId())).isEmpty();
        mockMvc.perform(get("/api/v1/crews").session(sessionB)).andExpect(status().isUnauthorized());
    }

    @Test
    void 탈퇴_트랜잭션이_실패하면_사용자와_크루와_소속이_모두_보존된다() {
        User target = saveUser("rollback@example.com", "롤백사용자");
        Crew managed = crewRepository.save(Crew.create("롤백크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.saveAndFlush(CrewUser.manager(target, managed));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            userService.withdraw(target.getId());
            throw new IllegalStateException("응답 전 실패 가정");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.existsById(target.getId())).isTrue();
        assertThat(crewRepository.existsById(managed.getId())).isTrue();
        assertThat(crewUserRepository.findByCrewIdAndUserId(managed.getId(), target.getId())).isPresent();
    }

    private void login(MockHttpSession session) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"withdraw@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }
}
