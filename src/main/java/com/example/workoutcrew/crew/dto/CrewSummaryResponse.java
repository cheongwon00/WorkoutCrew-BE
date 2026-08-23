package com.example.workoutcrew.crew.dto;

import com.example.workoutcrew.crew.domain.Crew;

public record CrewSummaryResponse(
        Long id,
        String name,
        int maxUsers,
        long currentUsers,
        int weeklyCertificationGoal
) {
    public static CrewSummaryResponse of(Crew crew, long currentUsers) {
        return new CrewSummaryResponse(crew.getId(), crew.getName(), crew.getMaxUsers(), currentUsers,
                crew.getWeeklyCertificationGoal());
    }
}
