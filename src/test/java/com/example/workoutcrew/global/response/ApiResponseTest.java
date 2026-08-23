package com.example.workoutcrew.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-23T05:05:09Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void 성공_응답은_네_필드와_서울_시간을_사용한다() throws Exception {
        ApiResponse<String> response = ApiResponse.success(HttpStatus.OK, "성공", "조회값", clock);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);

        assertThat(fields)
                .containsExactlyInAnyOrder("status", "message", "data", "timestamp");
        assertThat(json.get("status").asInt()).isEqualTo(200);
        assertThat(json.get("timestamp").asText()).isEqualTo("2026-08-23T14:05:09");
    }

    @Test
    void 오류_응답의_data는_null이다() {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST, "실패", clock);
        assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.data()).isNull();
    }
}
