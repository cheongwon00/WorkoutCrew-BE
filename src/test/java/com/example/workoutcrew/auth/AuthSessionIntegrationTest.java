package com.example.workoutcrew.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AuthSessionIntegrationTest extends ApiIntegrationSupport {

    @Test
    void 두_세션_중_로그아웃한_현재_세션만_무효화된다() throws Exception {
        saveUser("multi@example.com", "다중기기");
        MockHttpSession sessionA = new MockHttpSession();
        MockHttpSession sessionB = new MockHttpSession();
        String originalSessionAId = sessionA.getId();
        login(sessionA);
        login(sessionB);
        assertThat(sessionA.getId()).isNotEqualTo(originalSessionAId);

        mockMvc.perform(post("/api/v1/auth/logout").session(sessionA).with(csrf()))
                .andExpect(status().isOk());
        assertThat(sessionA.isInvalid()).isTrue();
        mockMvc.perform(get("/api/v1/crews").session(sessionB)).andExpect(status().isOk());
    }

    private void login(MockHttpSession session) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"multi@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }
}
