CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname UNIQUE (nickname),
    CONSTRAINT ck_users_nickname_length CHECK (CHAR_LENGTH(nickname) BETWEEN 2 AND 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE crew (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    max_users INT NOT NULL,
    weekly_certification_goal INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_crew_name UNIQUE (name),
    CONSTRAINT ck_crew_name_length CHECK (CHAR_LENGTH(name) BETWEEN 2 AND 20),
    CONSTRAINT ck_crew_max_users CHECK (max_users BETWEEN 2 AND 100),
    CONSTRAINT ck_crew_weekly_goal CHECK (weekly_certification_goal BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE crew_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    crew_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,
    manager_crew_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_crew_user_membership UNIQUE (user_id, crew_id),
    CONSTRAINT uk_crew_user_single_manager UNIQUE (manager_crew_id),
    CONSTRAINT ck_crew_user_role CHECK (role IN ('MEMBER', 'MANAGER')),
    CONSTRAINT ck_crew_user_manager_marker CHECK (
        (role = 'MANAGER' AND manager_crew_id = crew_id)
        OR (role = 'MEMBER' AND manager_crew_id IS NULL)
    ),
    CONSTRAINT fk_crew_user_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_crew_user_crew FOREIGN KEY (crew_id) REFERENCES crew (id) ON DELETE CASCADE,
    INDEX ix_crew_user_crew_id (crew_id),
    INDEX ix_crew_user_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
