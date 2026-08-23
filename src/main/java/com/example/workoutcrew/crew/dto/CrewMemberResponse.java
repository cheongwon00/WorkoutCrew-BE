package com.example.workoutcrew.crew.dto;

import com.example.workoutcrew.crew.domain.CrewRole;
import com.example.workoutcrew.crew.domain.CrewUser;

public record CrewMemberResponse(Long userId, String nickname, CrewRole role) {
    public static CrewMemberResponse from(CrewUser membership) {
        return new CrewMemberResponse(membership.getUser().getId(), membership.getUser().getNickname(),
                membership.getRole());
    }
}
