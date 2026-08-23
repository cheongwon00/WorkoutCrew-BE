---

description: "WorkoutCrew MVP 상세 기능 구현 작업 목록"
---

# 작업 목록: WorkoutCrew MVP 상세 기능

**입력 문서**: `specs/001-mvp-detailed-spec/`의 명세 및 설계 산출물

**선행 문서**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/api-rules.md`,
`quickstart.md`

**테스트 원칙**: 기능 명세와 헌법이 자동화 테스트를 필수로 요구한다. 각 사용자 스토리의
테스트 작업을 먼저 작성하여 실패를 확인한 뒤 구현 작업을 수행한다.

**구성 원칙**: 사용자 스토리별로 구현과 검증을 완료할 수 있도록 작업을 묶고, 모든 작업에
실제 파일 경로를 명시한다.

## 형식: `[ID] [P?] [Story] 설명`

- **[P]**: 선행 조건을 충족한 후 다른 파일의 작업과 병렬 실행 가능
- **[Story]**: 기능 명세의 사용자 스토리 식별자
- Setup, Foundational, 마무리 단계에는 Story 표기를 사용하지 않는다.

## 1단계: 프로젝트 설정

**목적**: 데이터베이스 마이그레이션과 실제 MySQL 통합 테스트를 실행할 기반을 준비한다.

- [X] T001 `build.gradle`에 Flyway MySQL 마이그레이션과 Testcontainers MySQL/JUnit 테스트 의존성을 추가한다.
- [X] T002 [P] `src/main/resources/application.properties`에 환경 변수 기반 MySQL 연결, `ddl-auto=validate`, Flyway, Asia/Seoul 시간대, 세션 쿠키 설정을 추가한다.
- [X] T003 [P] `src/test/resources/application-test.properties`에 테스트 프로필과 결정적인 테스트 시간대 설정을 작성한다.
- [X] T004 [P] `src/main/resources/db/migration/V1__create_workoutcrew_schema.sql`에 users, crew, crew_user 테이블과 고유·CHECK·외래 키·조건부 관리자 인덱스를 정의한다.
- [X] T005 `src/test/java/com/example/workoutcrew/support/MySqlContainerSupport.java`에 전체 통합 테스트가 공유할 MySQL Testcontainer와 동적 속성 등록 기반을 구현한다.

**체크포인트**: 빈 MySQL에서 마이그레이션이 성공하고 테스트 컨테이너가 Spring 컨텍스트에 연결된다.

---

## 2단계: 공통 기반

**목적**: 모든 사용자 스토리가 공유하는 엔티티, 저장소, 응답, 예외, 시간 및 비밀번호 기반을 만든다.

**중요**: 이 단계가 완료되기 전에는 사용자 스토리 구현을 시작하지 않는다.

### 선행 테스트

- [X] T006 [P] `src/test/java/com/example/workoutcrew/global/response/ApiResponseTest.java`에 네 개 최상위 필드, status 일치, data null 규칙, 고정 Clock 타임스탬프 직렬화 실패 테스트를 작성한다.
- [X] T007 [P] `src/test/java/com/example/workoutcrew/support/DatabaseConstraintIntegrationTest.java`에 이메일·닉네임·크루 이름·소속 복합 고유성, 범위 CHECK, 단일 관리자, 외래 키 삭제 정책 실패 테스트를 작성한다.

### 공통 구현

- [X] T008 [P] `src/main/java/com/example/workoutcrew/user/domain/User.java`에 사용자 필드, 생성 규칙, 닉네임 변경 행위를 구현한다.
- [X] T009 [P] `src/main/java/com/example/workoutcrew/crew/domain/Crew.java`에 크루 필드, 생성 규칙, 부분 수정 및 최대 인원 검증 행위를 구현한다.
- [X] T010 [P] `src/main/java/com/example/workoutcrew/crew/domain/CrewRole.java`에 MEMBER와 MANAGER 역할을 정의한다.
- [X] T011 `src/main/java/com/example/workoutcrew/crew/domain/CrewUser.java`에 User-Crew 소속 관계와 역할 전이 행위를 구현한다.
- [X] T012 [P] `src/main/java/com/example/workoutcrew/user/repository/UserRepository.java`에 이메일·닉네임 조회와 사용자 쓰기 잠금 쿼리를 정의한다.
- [X] T013 [P] `src/main/java/com/example/workoutcrew/crew/repository/CrewRepository.java`에 이름 조회, 페이지 조회, 크루 쓰기 잠금 쿼리를 정의한다.
- [X] T014 `src/main/java/com/example/workoutcrew/crew/repository/CrewUserRepository.java`에 소속·역할 조회, 인원 집계, 사용자와 크루별 삭제 쿼리를 정의한다.
- [X] T015 [P] `src/main/java/com/example/workoutcrew/global/response/ApiResponse.java`에 status, message, data, timestamp만 갖는 성공·오류 응답 팩터리를 구현한다.
- [X] T016 [P] `src/main/java/com/example/workoutcrew/global/response/PageData.java`에 content, page, size, totalElements, totalPages 조회 응답을 구현한다.
- [X] T017 [P] `src/main/java/com/example/workoutcrew/global/config/TimeConfig.java`에 Asia/Seoul 운영 Clock과 테스트에서 교체 가능한 Clock 빈을 구성한다.
- [X] T018 [P] `src/main/java/com/example/workoutcrew/global/exception/ErrorCode.java`와 `src/main/java/com/example/workoutcrew/global/exception/BusinessException.java`에 400·401·403·404·409 오류 분류와 외부 메시지를 정의한다.
- [X] T019 `src/main/java/com/example/workoutcrew/global/exception/GlobalExceptionHandler.java`에 검증·JSON·도메인·메서드·미디어 타입·예상 밖 오류를 공통 응답으로 변환하는 처리를 구현한다.
- [X] T020 [P] `src/main/java/com/example/workoutcrew/global/config/PasswordConfig.java`에 DelegatingPasswordEncoder 빈을 구성한다.
- [X] T021 `src/test/java/com/example/workoutcrew/WorkoutcrewApplicationTests.java`를 MySQL 컨테이너와 V1 마이그레이션을 사용하는 전체 컨텍스트 기동 테스트로 갱신한다.
- [X] T022 `src/test/java/com/example/workoutcrew/global/response/ApiResponseTest.java`, `src/test/java/com/example/workoutcrew/support/DatabaseConstraintIntegrationTest.java`, `src/test/java/com/example/workoutcrew/WorkoutcrewApplicationTests.java`를 실행하여 공통 응답, DB 방어 계층, 전체 컨텍스트 기동이 통과하는지 확인한다.

**체크포인트**: 공통 응답과 세 엔티티의 저장·제약이 동작하고 이후 스토리가 같은 기반을 사용할 수 있다.

---

## 3단계: 사용자 스토리 1 - 회원가입과 인증 (우선순위: P1) 🎯 MVP

**목표**: 신규 사용자가 가입하고 여러 기기에서 로그인하며 현재 기기만 로그아웃할 수 있다.

**독립 테스트 기준**: 신규 계정의 가입·로그인·로그아웃, 입력 경계와 중복, 동일 계정의 두
세션 중 한 세션만 로그아웃되는 동작을 다른 크루 기능 없이 검증한다.

### 테스트

- [X] T023 [P] [US1] `src/test/java/com/example/workoutcrew/user/domain/UserTest.java`에 이메일·비밀번호·닉네임 경계와 비밀번호 원문 비저장 기대 테스트를 작성한다.
- [X] T024 [P] [US1] `src/test/java/com/example/workoutcrew/user/controller/UserSignUpControllerTest.java`에 회원가입 201·Location·data null, 400, 409 응답 계약 테스트를 작성한다.
- [X] T025 [P] [US1] `src/test/java/com/example/workoutcrew/auth/controller/AuthControllerTest.java`에 CSRF 조회, 로그인 200/401, 로그아웃 200, 공통 응답 및 CSRF 403 테스트를 작성한다.
- [X] T026 [P] [US1] `src/test/java/com/example/workoutcrew/auth/AuthSessionIntegrationTest.java`에 두 세션 로그인, 세션 ID 회전, 현재 세션 로그아웃 후 다른 세션 유지 시나리오를 작성한다.

### 구현

- [X] T027 [P] [US1] `src/main/java/com/example/workoutcrew/user/dto/SignUpRequest.java`에 email, password, nickname의 구조적 검증을 정의한다.
- [X] T028 [P] [US1] `src/main/java/com/example/workoutcrew/auth/dto/LoginRequest.java`와 `src/main/java/com/example/workoutcrew/auth/dto/CsrfTokenResponse.java`에 로그인 및 CSRF 조회 계약을 정의한다.
- [X] T029 [US1] `src/main/java/com/example/workoutcrew/user/service/UserService.java`에 중복 검사, 비밀번호 인코딩, 사용자 저장을 하나의 회원가입 유스케이스로 구현한다.
- [X] T030 [P] [US1] `src/main/java/com/example/workoutcrew/global/security/CustomPrincipal.java`에 불변 userId 기반 세션 principal과 equals/hashCode를 구현한다.
- [X] T031 [US1] `src/main/java/com/example/workoutcrew/auth/service/CustomUserDetailsService.java`에 이메일 기반 사용자 조회와 공통 로그인 실패 처리를 구현한다.
- [X] T032 [US1] `src/main/java/com/example/workoutcrew/global/security/JsonLoginAuthenticationFilter.java`에 JSON 자격 증명 인증, 세션 전략 호출, SecurityContext 명시 저장을 구현한다.
- [X] T033 [P] [US1] `src/main/java/com/example/workoutcrew/global/security/ApiAuthenticationEntryPoint.java`, `src/main/java/com/example/workoutcrew/global/security/ApiAccessDeniedHandler.java`, `src/main/java/com/example/workoutcrew/global/security/ApiLogoutSuccessHandler.java`에 401·403·로그아웃 공통 응답을 구현한다.
- [X] T034 [US1] `src/main/java/com/example/workoutcrew/global/security/SecurityConfig.java`에 세션 인증, 무제한 동시 세션 추적, CSRF 쿠키, JSON 로그인, 현재 세션 로그아웃, 공개 경로와 보호 경로를 구성한다.
- [X] T035 [US1] `src/main/java/com/example/workoutcrew/user/controller/UserController.java`와 `src/main/java/com/example/workoutcrew/auth/controller/AuthController.java`에 회원가입과 CSRF 토큰 조회 엔드포인트를 구현하고 US1 테스트 전체를 통과시킨다.

**체크포인트**: 사용자 스토리 1이 독립적으로 동작하며 인증 기반 MVP를 시연할 수 있다.

---

## 4단계: 사용자 스토리 2 - 회원 정보와 계정 생명주기 관리 (우선순위: P1)

**목표**: 인증 사용자가 닉네임을 변경하고, 관리 크루와 일반 소속을 원자적으로 정리하며 회원탈퇴할 수 있다.

**독립 테스트 기준**: 인증된 사용자에게 미리 구성한 소속 데이터를 제공하고 닉네임 변경,
중복 실패, 일반 소속 제거, 관리 크루 삭제, 모든 기기 세션 만료를 검증한다.

### 테스트

- [X] T036 [P] [US2] `src/test/java/com/example/workoutcrew/user/controller/UserProfileControllerTest.java`에 닉네임 PATCH와 회원탈퇴 DELETE의 200·400·401·409 및 data null 계약 테스트를 작성한다.
- [X] T037 [P] [US2] `src/test/java/com/example/workoutcrew/user/service/UserServiceTest.java`에 동일 닉네임 무변경 성공, 중복 롤백, 관리 크루와 일반 소속 정리 순서 테스트를 작성한다.
- [X] T038 [P] [US2] `src/test/java/com/example/workoutcrew/user/UserWithdrawalIntegrationTest.java`에 여러 관리 크루 삭제, 일반 소속 제거, DB 롤백, 커밋 후 모든 기기 세션 만료 시나리오를 작성한다.

### 구현

- [X] T039 [P] [US2] `src/main/java/com/example/workoutcrew/user/dto/NicknameUpdateRequest.java`에 2~10자 닉네임 검증을 정의한다.
- [X] T040 [P] [US2] `src/main/java/com/example/workoutcrew/user/service/UserWithdrawalCommittedEvent.java`와 `src/main/java/com/example/workoutcrew/global/security/UserSessionService.java`에 커밋 후 전체 세션 만료 계약을 구현한다.
- [X] T041 [US2] `src/main/java/com/example/workoutcrew/user/repository/UserRepository.java`, `src/main/java/com/example/workoutcrew/crew/repository/CrewRepository.java`, `src/main/java/com/example/workoutcrew/crew/repository/CrewUserRepository.java`에 회원탈퇴용 사용자·관리 크루 순차 잠금과 일반 소속 정리 쿼리를 추가한다.
- [X] T042 [US2] `src/main/java/com/example/workoutcrew/user/service/UserService.java`에 닉네임 변경과 관리 크루·일반 소속·사용자 하드 삭제를 하나의 트랜잭션으로 구현하고 커밋 이벤트를 발행한다.
- [X] T043 [US2] `src/main/java/com/example/workoutcrew/global/security/UserSessionService.java`에 탈퇴 커밋 이벤트의 동기 처리와 완료 응답 전 모든 SessionRegistry 항목 만료를 구현한다.
- [X] T044 [US2] `src/main/java/com/example/workoutcrew/user/controller/UserController.java`에 `PATCH /api/v1/users/me`와 `DELETE /api/v1/users/me`를 추가한다.
- [X] T045 [US2] `src/test/java/com/example/workoutcrew/user/UserWithdrawalIntegrationTest.java`를 포함한 US2 테스트를 실행하여 데이터와 세션 정리가 전부 성공하거나 전부 보존되는지 확인한다.

**체크포인트**: 사용자 스토리 2가 인증 기반 위에서 독립 검증되며 관리자 없는 크루를 남기지 않는다.

---

## 5단계: 사용자 스토리 3 - 크루 생성과 탐색 (우선순위: P1)

**목표**: 인증 사용자가 유효한 크루를 생성해 유일한 관리자가 되고 페이지 목록에서 크루를 탐색한다.

**독립 테스트 기준**: 인증 사용자 한 명으로 크루 생성 성공·검증 실패·원자적 롤백과 비밀번호
비노출 목록의 기본 페이지·정렬·빈 결과를 검증한다.

### 테스트

- [X] T046 [P] [US3] `src/test/java/com/example/workoutcrew/crew/domain/CrewTest.java`에 이름·가입 비밀번호·최대 인원·주간 목표 경계와 동일 값 수정 행위 테스트를 작성한다.
- [X] T047 [P] [US3] `src/test/java/com/example/workoutcrew/crew/repository/CrewRepositoryTest.java`에 이름 고유성, id 정렬 페이지 조회, 크루 쓰기 잠금 쿼리 테스트를 작성한다.
- [X] T048 [P] [US3] `src/test/java/com/example/workoutcrew/crew/controller/CrewCreateListControllerTest.java`에 생성 201·Location·data null 및 목록 200·페이지 data·401·검증 오류 계약 테스트를 작성한다.
- [X] T049 [P] [US3] `src/test/java/com/example/workoutcrew/crew/CrewCreationIntegrationTest.java`에 크루와 생성자 MANAGER 관계의 동시 생성 및 실패 시 전체 롤백을 작성한다.

### 구현

- [X] T050 [P] [US3] `src/main/java/com/example/workoutcrew/crew/dto/CrewCreateRequest.java`와 `src/main/java/com/example/workoutcrew/crew/dto/CrewSummaryResponse.java`에 생성 입력과 비밀번호 없는 목록 항목 계약을 정의한다.
- [X] T051 [US3] `src/main/java/com/example/workoutcrew/crew/repository/CrewRepository.java`와 `src/main/java/com/example/workoutcrew/crew/repository/CrewUserRepository.java`에 이름 중복, 기본 id 내림차순 페이지, 현재 인원 집계 조회를 구현한다.
- [X] T052 [US3] `src/main/java/com/example/workoutcrew/crew/service/CrewService.java`에 비밀번호 인코딩, 크루와 생성자 MANAGER 관계 원자 생성, 페이지 목록 조회를 구현한다.
- [X] T053 [US3] `src/main/java/com/example/workoutcrew/crew/controller/CrewController.java`에 `POST /api/v1/crews`와 `GET /api/v1/crews`를 구현한다.
- [X] T054 [US3] `src/main/java/com/example/workoutcrew/crew/controller/CrewController.java`에서 page 기본 0, size 기본 20·최대 100, id 정렬 허용값, 빈 목록 200을 강제한다.
- [X] T055 [US3] `src/test/java/com/example/workoutcrew/crew/CrewCreationIntegrationTest.java`를 포함한 US3 테스트를 실행하여 생성·목록 계약과 유일 관리자 조건을 확인한다.

**체크포인트**: 사용자 스토리 3이 독립적으로 크루 생성과 탐색 가치를 제공한다.

---

## 6단계: 사용자 스토리 4 - 크루 가입과 소속 조회 (우선순위: P1)

**목표**: 사용자가 정원 내에서 크루에 가입하고 소속자 목록을 조회하며 역할에 맞게 탈퇴한다.

**독립 테스트 기준**: 기존 사용자와 크루 fixture로 정상 가입·중복·비밀번호·정원·동시 가입,
소속자 전용 목록, MEMBER 관계 삭제, MANAGER 크루 전체 삭제를 검증한다.

### 테스트

- [X] T056 [P] [US4] `src/test/java/com/example/workoutcrew/crew/service/CrewMembershipServiceTest.java`에 가입 검증 순서, MEMBER 탈퇴, MANAGER 탈퇴 시 크루 삭제, 권한 실패 테스트를 작성한다.
- [X] T057 [P] [US4] `src/test/java/com/example/workoutcrew/crew/controller/CrewMembershipControllerTest.java`에 가입 201, 목록 200, 탈퇴 200, 400·401·403·404·409 및 data 규칙 테스트를 작성한다.
- [X] T058 [P] [US4] `src/test/java/com/example/workoutcrew/crew/CrewJoinConcurrencyIntegrationTest.java`에 마지막 한 자리 동시 가입과 동일 사용자의 동시 중복 가입 시나리오를 실제 MySQL로 작성한다.

### 구현

- [X] T059 [P] [US4] `src/main/java/com/example/workoutcrew/crew/dto/CrewJoinRequest.java`와 `src/main/java/com/example/workoutcrew/crew/dto/CrewMemberResponse.java`에 가입 비밀번호와 이메일 없는 크루원 응답 계약을 정의한다.
- [X] T060 [US4] `src/main/java/com/example/workoutcrew/crew/repository/CrewRepository.java`와 `src/main/java/com/example/workoutcrew/crew/repository/CrewUserRepository.java`에 크루 잠금 후 중복·인원·소속·역할을 재조회하는 쿼리를 추가한다.
- [X] T061 [US4] `src/main/java/com/example/workoutcrew/crew/service/CrewMembershipService.java`에 가입, 소속자 페이지 조회, MEMBER 관계 삭제, MANAGER 크루 삭제 트랜잭션을 구현한다.
- [X] T062 [US4] `src/main/java/com/example/workoutcrew/crew/controller/CrewMemberController.java`에 `POST/GET /api/v1/crews/{crewId}/members`와 `DELETE /api/v1/crews/{crewId}/members/me`를 구현한다.
- [X] T063 [US4] `src/test/java/com/example/workoutcrew/crew/CrewJoinConcurrencyIntegrationTest.java`를 포함한 US4 테스트를 실행하여 정원·중복·소속 권한 불변 조건을 확인한다.

**체크포인트**: 사용자 스토리 4가 정원과 역할 무결성을 보존하며 독립적으로 가입·조회·탈퇴를 제공한다.

---

## 7단계: 사용자 스토리 5 - 크루와 크루원 관리 (우선순위: P2)

**목표**: MANAGER가 크루 수정·삭제·위임·추방을 수행하고 모든 소속자가 크루원 목록을 조회한다.

**독립 테스트 기준**: MANAGER와 MEMBER fixture로 역할별 200/403, 유효·무효 수정, 원자적
관리자 위임, 추방, 삭제 및 동시 요청 후 유일 관리자 조건을 검증한다.

### 테스트

- [X] T064 [P] [US5] `src/test/java/com/example/workoutcrew/crew/service/CrewManagementServiceTest.java`에 부분 수정, 현재 인원보다 작은 정원, 삭제, 위임, 추방의 권한·상태·롤백 테스트를 작성한다.
- [X] T065 [P] [US5] `src/test/java/com/example/workoutcrew/crew/controller/CrewManagementControllerTest.java`에 PATCH·DELETE 관리 엔드포인트의 200·400·401·403·404·409와 data null 계약 테스트를 작성한다.
- [X] T066 [P] [US5] `src/test/java/com/example/workoutcrew/crew/CrewManagerConcurrencyIntegrationTest.java`에 동시 위임, 위임과 추방·탈퇴 경쟁, 정원 수정과 가입 경쟁 시나리오를 실제 MySQL로 작성한다.

### 구현

- [X] T067 [P] [US5] `src/main/java/com/example/workoutcrew/crew/dto/CrewUpdateRequest.java`와 `src/main/java/com/example/workoutcrew/crew/dto/ManagerTransferRequest.java`에 부분 수정과 위임 대상 검증을 정의한다.
- [X] T068 [US5] `src/main/java/com/example/workoutcrew/crew/repository/CrewRepository.java`와 `src/main/java/com/example/workoutcrew/crew/repository/CrewUserRepository.java`에 일관된 ID 순서 잠금, 관리자·대상 MEMBER 재조회, 삭제 쿼리를 추가한다.
- [X] T069 [US5] `src/main/java/com/example/workoutcrew/crew/service/CrewManagementService.java`에 크루 수정·삭제, 기존 관리자 강등 flush 후 대상 승격, MEMBER 추방 트랜잭션을 구현한다.
- [X] T070 [US5] `src/main/java/com/example/workoutcrew/crew/controller/CrewController.java`에 `PATCH/DELETE /api/v1/crews/{crewId}`를 추가한다.
- [X] T071 [US5] `src/main/java/com/example/workoutcrew/crew/controller/CrewMemberController.java`에 `PATCH /api/v1/crews/{crewId}/manager`와 `DELETE /api/v1/crews/{crewId}/members/{userId}`를 추가한다.
- [X] T072 [US5] `src/test/java/com/example/workoutcrew/crew/CrewManagerConcurrencyIntegrationTest.java`를 포함한 US5 테스트를 실행하여 권한표와 유일 관리자 불변 조건을 확인한다.

**체크포인트**: 다섯 사용자 스토리와 원본 14개 비즈니스 기능이 모두 구현되고 독립 검증된다.

---

## 8단계: 마무리 및 횡단 관심사

**목적**: 전체 스토리의 보안, 계약, 성능, 재시도 및 문서 일관성을 최종 검증한다.

- [X] T073 [P] `src/test/java/com/example/workoutcrew/global/security/SecurityAuthorizationIntegrationTest.java`에 비인증·비소속·MEMBER·MANAGER 권한표와 모든 상태 변경의 CSRF 검증을 작성한다.
- [X] T074 [P] `src/test/java/com/example/workoutcrew/global/response/FullApiResponseContractTest.java`에 14개 비즈니스 작업과 CSRF 지원 작업의 최상위 키, status, message, data, timestamp 계약을 검증한다.
- [X] T075 [P] `src/test/java/com/example/workoutcrew/crew/CrewPaginationPerformanceTest.java`에 기본·최대 페이지 제한, 정렬, 빈 페이지, 정상 환경 3초 이내 결과를 검증한다.
- [X] T076 `src/main/java/com/example/workoutcrew/global/config/RetryConfig.java`와 `src/test/java/com/example/workoutcrew/crew/DeadlockRetryIntegrationTest.java`에 교착·잠금 실패의 제한 재시도와 최종 409 변환을 구현·검증한다.
- [X] T077 [P] `src/main/resources/application.properties`와 `src/main/java/com/example/workoutcrew/global/security/SecurityConfig.java`에서 운영 쿠키 Secure·HttpOnly·SameSite, 비밀값 환경 변수화, 로그 민감정보 비노출을 점검한다.
- [X] T078 `specs/001-mvp-detailed-spec/contracts/api-rules.md`와 `specs/001-mvp-detailed-spec/quickstart.md`를 실제 구현의 경로·상태·메시지·검증 명령과 최종 대조하여 한글 문서를 갱신한다.
- [X] T079 `specs/001-mvp-detailed-spec/validation.md`에 `./gradlew test`와 quickstart 전체 흐름의 실행 결과, 14개 기능 통과 여부, 남은 위험을 한글로 기록한다.

---

## 의존성과 실행 순서

### 단계 의존성

- **1단계 설정**: 즉시 시작할 수 있다.
- **2단계 공통 기반**: 1단계 완료 후 시작하며 모든 사용자 스토리를 차단한다.
- **3단계 US1**: 공통 기반 완료 후 시작한다. 인증 주체와 세션을 제공하므로 나머지 스토리의 선행 조건이다.
- **4단계 US2**: US1 완료 후 시작한다. fixture로 Crew/CrewUser를 구성해 독립 검증한다.
- **5단계 US3**: US1 완료 후 시작하며 US2와 병렬 진행할 수 있다.
- **6단계 US4**: US1과 US3 완료 후 시작한다.
- **7단계 US5**: US3과 US4 완료 후 시작한다.
- **8단계 마무리**: 배포 범위에 포함된 모든 사용자 스토리 완료 후 시작한다.

### 사용자 스토리 의존성 그래프

```text
설정 → 공통 기반 → US1 회원가입·인증
                       ├─→ US2 회원 정보·회원탈퇴
                       └─→ US3 크루 생성·탐색 → US4 가입·소속 → US5 크루 관리
```

US2의 관리자 회원탈퇴 검증은 공통 기반의 Crew/CrewUser fixture를 사용하므로 US3 구현과
독립적이다. US4는 가입할 크루 생성 기능이 필요하고 US5는 소속 및 역할 전이가 필요하므로
각각 앞선 스토리를 기능 의존성으로 갖는다.

### 스토리 내부 순서

1. 테스트를 먼저 작성하고 예상한 이유로 실패하는지 확인한다.
2. 요청·응답 DTO와 도메인 행위를 구현한다.
3. 저장소 쿼리와 잠금 규칙을 구현한다.
4. 서비스 트랜잭션과 권한 검증을 구현한다.
5. 컨트롤러 또는 보안 필터를 연결한다.
6. 해당 스토리의 전체 테스트를 통과시킨다.

## 병렬 실행 기회

- 1단계에서는 T002, T003, T004를 병렬로 진행할 수 있다.
- 2단계에서는 T006/T007, T008/T009/T010, T012/T013, T015/T016/T017/T018/T020 묶음을
  각 선행 조건이 충족된 시점에 병렬로 진행할 수 있다.
- 공통 기반과 US1 완료 후 US2와 US3을 서로 병렬로 진행할 수 있다.
- 각 스토리의 `[P]` 테스트와 DTO 작업은 같은 단계 안에서 병렬 실행할 수 있다.
- 마무리 단계의 T073, T074, T075, T077은 서로 다른 파일에서 병렬 진행할 수 있다.

### 사용자 스토리별 병렬 실행 예시

```text
US1: T023 UserTest | T024 UserSignUpControllerTest | T025 AuthControllerTest | T026 AuthSessionIntegrationTest
US2: T036 UserProfileControllerTest | T037 UserServiceTest | T038 UserWithdrawalIntegrationTest
US3: T046 CrewTest | T047 CrewRepositoryTest | T048 CrewCreateListControllerTest | T049 CrewCreationIntegrationTest
US4: T056 CrewMembershipServiceTest | T057 CrewMembershipControllerTest | T058 CrewJoinConcurrencyIntegrationTest
US5: T064 CrewManagementServiceTest | T065 CrewManagementControllerTest | T066 CrewManagerConcurrencyIntegrationTest
```

## 구현 전략

### 인증 MVP 우선

1. 1단계 프로젝트 설정을 완료한다.
2. 2단계 공통 기반을 완료한다.
3. 3단계 사용자 스토리 1을 완료한다.
4. US1 테스트를 독립 실행하고 회원가입·다기기 로그인·현재 기기 로그아웃을 시연한다.
5. 안정적인 인증 기반을 확인한 뒤 다음 스토리로 진행한다.

### 점진적 제공

1. 설정 + 공통 기반 → 기술 기반 완료
2. US1 → 회원가입과 인증 제공
3. US2와 US3 → 계정 생명주기와 크루 생성·탐색 제공
4. US4 → 크루 가입·조회·탈퇴 제공
5. US5 → 크루 수정·삭제·위임·추방 제공
6. 마무리 → 전체 계약·보안·성능 검증

각 단계는 이전 스토리의 테스트를 다시 실행하고 새 스토리의 독립 테스트를 통과한 뒤에만
완료로 처리한다.

## 참고

- `[P]` 작업은 선행 조건 충족 후 서로 다른 파일에서 병렬 실행할 수 있다.
- `[USn]`은 기능 명세의 사용자 스토리와 추적 가능성을 제공한다.
- 각 테스트 작업은 구현 전에 작성하고 의도한 이유로 실패하는지 확인한다.
- 각 작업 또는 논리적으로 결합된 소규모 작업 묶음이 끝날 때 커밋한다.
- 공개 인터페이스 변경 시 `contracts/api-rules.md`와 계약 테스트를 함께 갱신한다.
- 헌법을 위반하는 변경은 구현하지 않고 먼저 헌법 개정을 요청한다.
