package com.example.workoutcrew.user.controller;

import com.example.workoutcrew.global.response.ApiResponse;
import com.example.workoutcrew.global.security.CustomPrincipal;
import com.example.workoutcrew.user.dto.NicknameUpdateRequest;
import com.example.workoutcrew.user.dto.SignUpRequest;
import com.example.workoutcrew.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final Clock clock;

    public UserController(UserService userService, Clock clock) {
        this.userService = userService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        Long userId = userService.signUp(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + userId))
                .body(ApiResponse.success(HttpStatus.CREATED, "회원가입이 완료되었습니다.", null, clock));
    }

    @PatchMapping("/me")
    public ApiResponse<Void> updateNickname(@AuthenticationPrincipal CustomPrincipal principal,
                                            @Valid @RequestBody NicknameUpdateRequest request) {
        userService.updateNickname(principal.userId(), request.nickname());
        return ApiResponse.success(HttpStatus.OK, "닉네임이 수정되었습니다.", null, clock);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal CustomPrincipal principal) {
        userService.withdraw(principal.userId());
        return ApiResponse.success(HttpStatus.OK, "회원탈퇴가 완료되었습니다.", null, clock);
    }
}
