package com.example.workoutcrew.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.workoutcrew.user.dto.SignUpRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UserTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 회원가입_이메일_비밀번호_닉네임의_모든_경계를_검증한다() {
        assertThat(validator.validate(new SignUpRequest("user@example.com", "a".repeat(8), "가나"))).isEmpty();
        assertThat(validator.validate(new SignUpRequest("user@example.com", "a".repeat(20), "가".repeat(10)))).isEmpty();
        assertThat(validator.validate(new SignUpRequest("invalid", "a".repeat(8), "가나"))).isNotEmpty();
        assertThat(validator.validate(new SignUpRequest("user@example.com", "a".repeat(7), "가나"))).isNotEmpty();
        assertThat(validator.validate(new SignUpRequest("user@example.com", "a".repeat(21), "가나"))).isNotEmpty();
        assertThat(validator.validate(new SignUpRequest("user@example.com", "a".repeat(8), "가"))).isNotEmpty();
        assertThat(validator.validate(new SignUpRequest("user@example.com", "a".repeat(8), "가".repeat(11)))).isNotEmpty();
    }

    @Test
    void 닉네임_경계값을_허용하고_범위_밖은_거부한다() {
        User min = User.create("a@example.com", "{noop}password", "가나");
        User max = User.create("b@example.com", "{noop}password", "가나다라마바사아자차");
        assertThat(min.getNickname()).hasSize(2);
        assertThat(max.getNickname()).hasSize(10);
        assertThatThrownBy(() -> User.create("c@example.com", "{noop}password", "가"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 저장할_비밀번호는_원문과_다른_인코딩값이다() {
        User user = User.create("a@example.com", "{bcrypt}encoded-value", "사용자");
        assertThat(user.getPassword()).isNotEqualTo("password123");
    }
}
