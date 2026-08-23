package com.example.workoutcrew.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import com.example.workoutcrew.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;

class FrontendSmokeTest extends ApiIntegrationSupport {

    @Test
    void 비인증_사용자도_검증_화면과_정적_자원을_열_수_있다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("WORKOUTCREW")));
        mockMvc.perform(get("/styles.css")).andExpect(status().isOk());
        mockMvc.perform(get("/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/crews")).andExpect(status().isUnauthorized());
    }
}
