package com.example.workoutcrew.crew.domain;

import com.example.workoutcrew.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "crew_user", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "crew_id"}))
public class CrewUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @Enumerated(EnumType.STRING)
    private CrewRole role;

    @Column(name = "manager_crew_id")
    private Long managerCrewId;

    protected CrewUser() {
    }

    private CrewUser(User user, Crew crew, CrewRole role) {
        if (user == null || crew == null || role == null) throw new IllegalArgumentException("소속 정보는 필수입니다.");
        this.user = user;
        this.crew = crew;
        this.role = role;
        this.managerCrewId = role == CrewRole.MANAGER ? crew.getId() : null;
    }

    public static CrewUser member(User user, Crew crew) { return new CrewUser(user, crew, CrewRole.MEMBER); }
    public static CrewUser manager(User user, Crew crew) { return new CrewUser(user, crew, CrewRole.MANAGER); }
    public void promoteToManager() {
        this.role = CrewRole.MANAGER;
        this.managerCrewId = crew.getId();
    }
    public void demoteToMember() {
        this.role = CrewRole.MEMBER;
        this.managerCrewId = null;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Crew getCrew() { return crew; }
    public CrewRole getRole() { return role; }
}
