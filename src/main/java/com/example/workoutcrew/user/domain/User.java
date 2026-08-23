package com.example.workoutcrew.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 10)
    private String nickname;

    protected User() {
    }

    private User(String email, String encodedPassword, String nickname) {
        requireLength(nickname, 2, 10, "닉네임");
        this.email = email;
        this.password = encodedPassword;
        this.nickname = nickname;
    }

    public static User create(String email, String encodedPassword, String nickname) {
        if (email == null || email.isBlank() || encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("이메일과 인코딩된 비밀번호는 필수입니다.");
        }
        return new User(email, encodedPassword, nickname);
    }

    public void changeNickname(String nickname) {
        requireLength(nickname, 2, 10, "닉네임");
        this.nickname = nickname;
    }

    private static void requireLength(String value, int min, int max, String name) {
        if (value == null || value.length() < min || value.length() > max) {
            throw new IllegalArgumentException(name + " 길이가 올바르지 않습니다.");
        }
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
}
