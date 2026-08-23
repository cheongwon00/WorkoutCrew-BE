package com.example.workoutcrew.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;

@JsonPropertyOrder({"status", "message", "data", "timestamp"})
public record ApiResponse<T>(int status, String message, T data, String timestamp) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data, Clock clock) {
        return of(status.value(), message, data, clock);
    }

    public static ApiResponse<Void> error(HttpStatus status, String message, Clock clock) {
        return of(status.value(), message, null, clock);
    }

    public static <T> ApiResponse<T> of(int status, String message, T data, Clock clock) {
        return new ApiResponse<>(status, message, data, LocalDateTime.now(clock).format(FORMATTER));
    }
}
