package com.example.workoutcrew.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.workoutcrew.global.exception.BusinessException;
import com.example.workoutcrew.support.ApiIntegrationSupport;
import com.example.workoutcrew.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserServiceTest extends ApiIntegrationSupport {

    @Autowired UserService userService;

    @Test
    void 같은_닉네임은_성공하고_중복_닉네임은_롤백된다() {
        User user = saveUser("user@example.com", "현재닉네임");
        saveUser("other@example.com", "중복닉네임");
        userService.updateNickname(user.getId(), "현재닉네임");
        assertThatThrownBy(() -> userService.updateNickname(user.getId(), "중복닉네임"))
                .isInstanceOf(BusinessException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getNickname()).isEqualTo("현재닉네임");
    }
}
