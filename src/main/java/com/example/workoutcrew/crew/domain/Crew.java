package com.example.workoutcrew.crew.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "crew")
public class Crew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "max_users", nullable = false)
    private int maxUsers;

    @Column(name = "weekly_certification_goal", nullable = false)
    private int weeklyCertificationGoal;

    protected Crew() {
    }

    private Crew(String name, String encodedPassword, int maxUsers, int weeklyCertificationGoal) {
        validateName(name);
        validateMaxUsers(maxUsers);
        validateGoal(weeklyCertificationGoal);
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("인코딩된 비밀번호는 필수입니다.");
        }
        this.name = name;
        this.password = encodedPassword;
        this.maxUsers = maxUsers;
        this.weeklyCertificationGoal = weeklyCertificationGoal;
    }

    public static Crew create(String name, String encodedPassword, int maxUsers, int weeklyCertificationGoal) {
        return new Crew(name, encodedPassword, maxUsers, weeklyCertificationGoal);
    }

    public void update(String name, String encodedPassword, Integer maxUsers,
                       Integer weeklyCertificationGoal, long currentUsers) {
        if (name != null) {
            validateName(name);
            this.name = name;
        }
        if (encodedPassword != null) {
            if (encodedPassword.isBlank()) throw new IllegalArgumentException("비밀번호는 비어 있을 수 없습니다.");
            this.password = encodedPassword;
        }
        if (maxUsers != null) {
            validateMaxUsers(maxUsers);
            if (maxUsers < currentUsers) throw new IllegalStateException("현재 인원보다 정원을 줄일 수 없습니다.");
            this.maxUsers = maxUsers;
        }
        if (weeklyCertificationGoal != null) {
            validateGoal(weeklyCertificationGoal);
            this.weeklyCertificationGoal = weeklyCertificationGoal;
        }
    }

    private static void validateName(String name) {
        if (name == null || name.length() < 2 || name.length() > 20) {
            throw new IllegalArgumentException("크루 이름 길이가 올바르지 않습니다.");
        }
    }

    private static void validateMaxUsers(int maxUsers) {
        if (maxUsers < 2 || maxUsers > 100) throw new IllegalArgumentException("크루 정원 범위가 올바르지 않습니다.");
    }

    private static void validateGoal(int goal) {
        if (goal < 1 || goal > 7) throw new IllegalArgumentException("주간 인증 목표 범위가 올바르지 않습니다.");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public int getMaxUsers() { return maxUsers; }
    public int getWeeklyCertificationGoal() { return weeklyCertificationGoal; }
}
