package com.example.workoutcrew.crew.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CrewCreateListControllerTest extends ApiIntegrationSupport {

    @Test
    void 크루_생성은_201이고_목록은_비밀번호_없는_페이지다() throws Exception {
        User owner = saveUser("owner@example.com", "크루관리자");
        mockMvc.perform(post("/api/v1/crews").with(user(principal(owner))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"아침운동\",\"password\":\"crew12\",\"maxUsers\":10,\"weeklyCertificationGoal\":3}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(get("/api/v1/crews").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].name").value("아침운동"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0)).andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void 비인증과_잘못된_페이지와_생성값은_거부된다() throws Exception {
        User owner = saveUser("owner@example.com", "크루관리자");
        mockMvc.perform(get("/api/v1/crews")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/crews?size=101").with(user(principal(owner))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/crews").with(user(principal(owner))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"가\",\"password\":\"12\",\"maxUsers\":1,\"weeklyCertificationGoal\":8}"))
                .andExpect(status().isBadRequest());
    }
}
