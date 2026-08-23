package com.example.workoutcrew.user.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserProfileControllerTest extends ApiIntegrationSupport {

    @Test
    void 닉네임_수정과_회원탈퇴는_200과_data_null이다() throws Exception {
        User user = saveUser("profile@example.com", "기존닉네임");
        mockMvc.perform(patch("/api/v1/users/me").with(user(principal(user))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"수정닉네임\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(delete("/api/v1/users/me").with(user(principal(user))).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("회원탈퇴가 완료되었습니다."));
    }

    @Test
    void 잘못된_닉네임은_400_중복은_409_비인증은_401이다() throws Exception {
        User user = saveUser("profile@example.com", "기존닉네임");
        saveUser("other@example.com", "중복닉네임");
        mockMvc.perform(patch("/api/v1/users/me").with(user(principal(user))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"가\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/users/me").with(user(principal(user))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"중복닉네임\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/users/me").with(csrf())).andExpect(status().isUnauthorized());
    }
}
