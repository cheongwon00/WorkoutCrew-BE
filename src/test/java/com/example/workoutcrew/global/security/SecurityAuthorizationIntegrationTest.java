package com.example.workoutcrew.global.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityAuthorizationIntegrationTest extends ApiIntegrationSupport {

    @Test
    void 비인증_비소속_MEMBER_MANAGER_권한과_CSRF를_구분한다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "크루원");
        User outsider = saveUser("out@example.com", "외부인");
        Crew crew = crewRepository.save(Crew.create("권한크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(member, crew));

        mockMvc.perform(get("/api/v1/crews")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/crews/{id}/members", crew.getId()).with(user(principal(outsider))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/crews/{id}", crew.getId()).with(user(principal(member))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maxUsers\":6}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/crews/{id}", crew.getId()).with(user(principal(owner)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maxUsers\":6}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/crews/{id}", crew.getId()).with(user(principal(owner))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maxUsers\":6}"))
                .andExpect(status().isOk());
    }
}
