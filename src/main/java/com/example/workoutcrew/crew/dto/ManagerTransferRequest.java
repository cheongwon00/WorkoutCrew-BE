package com.example.workoutcrew.crew.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ManagerTransferRequest(@NotNull @Positive Long targetUserId) {
}
