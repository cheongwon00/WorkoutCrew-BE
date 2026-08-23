package com.example.workoutcrew.crew.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record CrewUpdateRequest(
        @Size(min = 2, max = 20) String name,
        @Size(min = 4, max = 8) String password,
        @Min(2) @Max(100) Integer maxUsers,
        @Min(1) @Max(7) Integer weeklyCertificationGoal
) {
    @AssertTrue(message = "수정할 항목이 하나 이상 필요합니다.")
    public boolean isAnyFieldProvided() {
        return name != null || password != null || maxUsers != null || weeklyCertificationGoal != null;
    }
}
