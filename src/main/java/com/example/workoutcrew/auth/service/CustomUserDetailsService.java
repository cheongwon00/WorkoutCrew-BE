package com.example.workoutcrew.auth.service;

import com.example.workoutcrew.global.security.CustomPrincipal;
import com.example.workoutcrew.user.domain.User;
import com.example.workoutcrew.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("인증에 실패했습니다."));
        return new CustomPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getNickname());
    }
}
