package com.example.workoutcrew.crew.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrewCreateRequest(
        @NotBlank @Size(min = 2, max = 20) String name,
        @NotBlank @Size(min = 4, max = 8) String password,
        @Min(2) @Max(100) int maxUsers,
        @Min(1) @Max(7) int weeklyCertificationGoal
) {
}
