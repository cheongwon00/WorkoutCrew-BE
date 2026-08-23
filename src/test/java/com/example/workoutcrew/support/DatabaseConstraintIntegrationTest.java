package com.example.workoutcrew.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseConstraintIntegrationTest extends MySqlContainerSupport {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM crew_user");
        jdbc.update("DELETE FROM crew");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void 사용자와_크루의_고유값과_범위를_DB가_방어한다() {
        insertUser("one@example.com", "닉네임1");
        assertThatThrownBy(() -> insertUser("one@example.com", "닉네임2"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertUser("two@example.com", "닉네임1"))
                .isInstanceOf(DataAccessException.class);

        insertCrew("크루하나", 2, 1);
        assertThatThrownBy(() -> insertCrew("크루하나", 3, 2)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertCrew("정원오류", 1, 1)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertCrew("목표오류", 2, 8)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void 소속_중복과_단일_관리자와_외래키_정책을_DB가_방어한다() {
        insertUser("one@example.com", "닉네임1");
        insertUser("two@example.com", "닉네임2");
        insertCrew("크루하나", 3, 1);
        Long crewId = jdbc.queryForObject("SELECT id FROM crew WHERE name='크루하나'", Long.class);
        Long user1 = jdbc.queryForObject("SELECT id FROM users WHERE email='one@example.com'", Long.class);
        Long user2 = jdbc.queryForObject("SELECT id FROM users WHERE email='two@example.com'", Long.class);
        insertMembership(user1, crewId, "MANAGER");

        assertThatThrownBy(() -> insertMembership(user1, crewId, "MEMBER"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertMembership(user2, crewId, "MANAGER"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM users WHERE id=?", user1))
                .isInstanceOf(DataAccessException.class);

        jdbc.update("DELETE FROM crew WHERE id=?", crewId);
        jdbc.update("DELETE FROM users WHERE id=?", user1);
    }

    private void insertUser(String email, String nickname) {
        jdbc.update("INSERT INTO users(email,password,nickname) VALUES (?,?,?)", email, "{noop}password", nickname);
    }

    private void insertCrew(String name, int maxUsers, int goal) {
        jdbc.update("INSERT INTO crew(name,password,max_users,weekly_certification_goal) VALUES (?,?,?,?)",
                name, "{noop}crew", maxUsers, goal);
    }

    private void insertMembership(Long userId, Long crewId, String role) {
        Long managerCrewId = "MANAGER".equals(role) ? crewId : null;
        jdbc.update("INSERT INTO crew_user(user_id,crew_id,role,manager_crew_id) VALUES (?,?,?,?)",
                userId, crewId, role, managerCrewId);
    }
}
