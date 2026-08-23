package com.example.workoutcrew.crew.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CrewTest {

    @Test
    void 이름_정원_주간목표의_경계값을_검증한다() {
        Crew min = Crew.create("운동", "{noop}crew", 2, 1);
        Crew max = Crew.create("가".repeat(20), "{noop}crew", 100, 7);
        assertThat(min.getMaxUsers()).isEqualTo(2);
        assertThat(max.getWeeklyCertificationGoal()).isEqualTo(7);
        assertThatThrownBy(() -> Crew.create("가", "{noop}crew", 2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Crew.create("정원", "{noop}crew", 101, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Crew.create("목표", "{noop}crew", 2, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 동일값_수정은_성공하고_현재인원보다_정원을_줄이면_실패한다() {
        Crew crew = Crew.create("운동크루", "{noop}crew", 10, 3);
        crew.update("운동크루", null, 10, 3, 4);
        assertThatThrownBy(() -> crew.update(null, null, 3, null, 4))
                .isInstanceOf(IllegalStateException.class);
    }
}
