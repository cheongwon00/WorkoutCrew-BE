package com.example.workoutcrew.auth.controller;

import com.example.workoutcrew.auth.dto.CsrfTokenResponse;
import com.example.workoutcrew.global.response.ApiResponse;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final Clock clock;

    public AuthController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ApiResponse.success(HttpStatus.OK, "CSRF 토큰을 조회했습니다.",
                CsrfTokenResponse.from(csrfToken), clock);
    }
}
