package com.example.workoutcrew.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(@NotBlank @Size(min = 2, max = 10) String nickname) {
}
