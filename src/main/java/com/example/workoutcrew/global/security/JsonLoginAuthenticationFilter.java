package com.example.workoutcrew.global.security;

import com.example.workoutcrew.auth.dto.LoginRequest;
import com.example.workoutcrew.global.exception.ErrorCode;
import com.example.workoutcrew.global.response.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

public class JsonLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Validator validator;

    public JsonLoginAuthenticationFilter(AuthenticationManager authenticationManager,
                                         ObjectMapper objectMapper, Clock clock, Validator validator) {
        super(PathPatternRequestMatcher.pathPattern(org.springframework.http.HttpMethod.POST,
                "/api/v1/auth/login"), authenticationManager);
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.validator = validator;
        setAuthenticationSuccessHandler(this::writeSuccess);
        setAuthenticationFailureHandler(this::writeFailure);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        if (request.getContentType() == null
                || !MediaType.parseMediaType(request.getContentType()).isCompatibleWith(MediaType.APPLICATION_JSON)) {
            throw new UnsupportedLoginMediaTypeException();
        }
        try {
            LoginRequest login = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
            if (!validator.validate(login).isEmpty()) {
                throw new InvalidLoginRequestException();
            }
            return getAuthenticationManager().authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login.email(), login.password()));
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidLoginRequestException(exception);
        }
    }

    private void writeSuccess(HttpServletRequest request, HttpServletResponse response,
                              Authentication authentication) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.success(HttpStatus.OK, "로그인이 완료되었습니다.", null, clock));
    }

    private void writeFailure(HttpServletRequest request, HttpServletResponse response,
                              AuthenticationException exception) throws IOException {
        ErrorCode code = exception instanceof UnsupportedLoginMediaTypeException
                ? ErrorCode.MEDIA_TYPE_NOT_SUPPORTED
                : exception instanceof InvalidLoginRequestException
                ? ErrorCode.INVALID_REQUEST
                : ErrorCode.LOGIN_FAILED;
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(code.status(), code.message(), clock));
    }

    private static final class InvalidLoginRequestException extends AuthenticationServiceException {
        InvalidLoginRequestException() { super("로그인 요청 형식 오류"); }
        InvalidLoginRequestException(Throwable cause) { super("로그인 요청 형식 오류", cause); }
    }

    private static final class UnsupportedLoginMediaTypeException extends AuthenticationServiceException {
        UnsupportedLoginMediaTypeException() { super("지원하지 않는 로그인 요청 형식"); }
    }
}
