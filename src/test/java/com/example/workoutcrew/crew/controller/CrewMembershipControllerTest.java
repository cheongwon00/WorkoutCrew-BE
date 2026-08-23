package com.example.workoutcrew.crew.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.crew.domain.CrewUser;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CrewMembershipControllerTest extends ApiIntegrationSupport {

    @Test
    void 가입_목록_탈퇴의_성공_계약과_소속_권한을_검증한다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "가입자");
        User outsider = saveUser("out@example.com", "외부인");
        Crew crew = crewRepository.save(Crew.create("멤버크루", passwordEncoder.encode("crew12"), 5, 3));
        crewUserRepository.saveAndFlush(CrewUser.manager(owner, crew));

        mockMvc.perform(post("/api/v1/crews/{id}/members", crew.getId())
                        .with(user(principal(member))).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"crew12\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(get("/api/v1/crews/{id}/members", crew.getId()).with(user(principal(member))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist());
        mockMvc.perform(get("/api/v1/crews/{id}/members", crew.getId()).with(user(principal(outsider))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/crews/{id}/members/me", crew.getId())
                        .with(user(principal(member))).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 잘못된_비밀번호와_중복_정원_초과를_상태코드로_구분한다() throws Exception {
        User owner = saveUser("owner@example.com", "관리자");
        User member = saveUser("member@example.com", "가입자");
        User extra = saveUser("extra@example.com", "추가자");
        Crew crew = crewRepository.save(Crew.create("정원크루", passwordEncoder.encode("crew12"), 2, 3));
        crewUserRepository.saveAndFlush(CrewUser.manager(owner, crew));
        mockMvc.perform(post("/api/v1/crews/{id}/members", crew.getId()).with(user(principal(member))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/crews/{id}/members", crew.getId()).with(user(principal(member))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"crew12\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/crews/{id}/members", crew.getId()).with(user(principal(member))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"crew12\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/crews/{id}/members", crew.getId()).with(user(principal(extra))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"crew12\"}"))
                .andExpect(status().isConflict());
    }
}
