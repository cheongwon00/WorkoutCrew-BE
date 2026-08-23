package com.example.workoutcrew.global.response;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FullApiResponseContractTest extends ApiIntegrationSupport {

    @Test
    void GET_변경성공_오류가_정확한_네_필드와_data_규칙을_사용한다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));

        mockMvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"contract@example.com\",\"password\":\"password123\",\"nickname\":\"계약사용자\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.status").value(201)).andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.data").doesNotExist());
    }
}
