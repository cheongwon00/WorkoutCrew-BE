package com.example.workoutcrew.crew.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CrewManagementControllerTest extends ApiIntegrationSupport {

    @Test
    void 수정_위임_추방_삭제는_MANAGER에게만_허용되고_data는_null이다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "크루원");
        Crew crew = crewRepository.save(Crew.create("관리크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.save(CrewUser.manager(owner, crew));
        crewUserRepository.saveAndFlush(CrewUser.member(member, crew));

        mockMvc.perform(patch("/api/v1/crews/{id}", crew.getId()).with(user(principal(member))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maxUsers\":6}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/crews/{id}", crew.getId()).with(user(principal(owner))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maxUsers\":6}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(delete("/api/v1/crews/{id}/members/{userId}", crew.getId(), member.getId())
                        .with(user(principal(owner))).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("크루원이 추방되었습니다."));
        mockMvc.perform(delete("/api/v1/crews/{id}", crew.getId()).with(user(principal(owner))).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void 빈_수정은_400_없는_크루는_404_CSRF_누락은_403이다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        Crew crew = crewRepository.save(Crew.create("관리크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.saveAndFlush(CrewUser.manager(owner, crew));
        mockMvc.perform(patch("/api/v1/crews/{id}", crew.getId()).with(user(principal(owner))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/crews/999999").with(user(principal(owner))).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/crews/{id}", crew.getId()).with(user(principal(owner))))
                .andExpect(status().isForbidden());
    }
}
