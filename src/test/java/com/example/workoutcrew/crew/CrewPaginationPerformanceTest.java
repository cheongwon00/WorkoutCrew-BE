package com.example.workoutcrew.crew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workoutcrew.crew.domain.Crew;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CrewPaginationPerformanceTest extends ApiIntegrationSupport {

    @Test
    void 기본_최대_정렬_빈페이지가_3초_안에_응답한다() throws Exception {
        User user = saveUser("page@example.com", "조회사용자");
        for (int index = 0; index < 30; index++) {
            crewRepository.save(Crew.create("페이지크루" + index, passwordEncoder.encode("crew12"), 10, 3));
        }
        crewRepository.flush();
        long started = System.nanoTime();
        mockMvc.perform(get("/api/v1/crews?size=100&sort=id,asc").with(user(principal(user))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.content.length()").value(30));
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(3));
        mockMvc.perform(get("/api/v1/crews?page=10").with(user(principal(user))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(0));
    }
}
