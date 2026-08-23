package com.example.workoutcrew.support;

import com.example.workoutcrew.crew.repository.CrewRepository;
import com.example.workoutcrew.crew.repository.CrewUserRepository;
import com.example.workoutcrew.global.security.CustomPrincipal;
import com.example.workoutcrew.user.domain.User;
import com.example.workoutcrew.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiIntegrationSupport extends MySqlContainerSupport {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected CrewRepository crewRepository;
    @Autowired protected CrewUserRepository crewUserRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetDatabase() {
        crewUserRepository.deleteAllInBatch();
        crewRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected User saveUser(String email, String nickname) {
        return userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("password123"), nickname));
    }

    protected CustomPrincipal principal(User user) {
        return new CustomPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getNickname());
    }
}
