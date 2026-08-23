package com.example.workoutcrew.global.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class CustomPrincipal implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String encodedPassword;
    private final String nickname;

    public CustomPrincipal(Long userId, String email, String encodedPassword, String nickname) {
        this.userId = userId;
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.nickname = nickname;
    }

    public Long userId() { return userId; }
    public String nickname() { return nickname; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String getPassword() { return encodedPassword; }
    @Override public String getUsername() { return email; }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof CustomPrincipal that && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId); }
}
