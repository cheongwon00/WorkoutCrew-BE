package com.example.workoutcrew.crew.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrewJoinRequest(@NotBlank @Size(min = 4, max = 8) String password) {
}
