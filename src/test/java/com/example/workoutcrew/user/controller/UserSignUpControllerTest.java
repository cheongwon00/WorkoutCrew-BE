package com.example.workoutcrew.user.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserSignUpControllerTest extends ApiIntegrationSupport {

    @Test
    void 회원가입은_201_Location_data_null을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"password123","nickname":"새사용자"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/users/\\d+")))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입력_오류는_400이고_중복은_409이다() throws Exception {
        saveUser("same@example.com", "기존사용자");
        mockMvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"short\",\"nickname\":\"가\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"same@example.com\",\"password\":\"password123\",\"nickname\":\"다른사용자\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }
}
