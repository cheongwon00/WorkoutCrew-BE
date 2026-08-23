# 구현 계획: WorkoutCrew MVP 상세 기능

**기능 식별자**: `001-mvp-detailed-spec` | **작성일**: 2026-08-23 |
**명세**: [spec.md](./spec.md)

**입력**: `specs/001-mvp-detailed-spec/spec.md`의 확정된 기능 명세

## 요약

회원가입·인증·회원 관리, 크루 생성·조회·수정·삭제, 가입·탈퇴·관리자 위임·크루원
조회·추방의 14개 MVP 기능을 단일 Spring Boot REST 서비스로 구현한다. 서버 측 세션으로
기기별 인증을 분리하고, 도메인별 계층 구조와 MySQL 제약 및 크루 단위 비관적 잠금으로
보안·정원·유일 관리자 불변 조건을 보장한다. 모든 성공과 오류 응답은 헌법의 네 필드 공통
계약을 사용한다.

## 기술 배경

**언어/버전**: Java 17

**주요 의존성**: Spring Boot 4.0.8, Spring Web MVC, Spring Security, Spring Data JPA,
Bean Validation, Lombok, MySQL Connector/J. 명시적 스키마 마이그레이션을 위한 Flyway와
실제 MySQL 통합 테스트를 위한 Testcontainers를 계획에 포함한다.

**저장소**: MySQL 8.x의 InnoDB. 사용자, 크루, 크루 소속을 영속화한다. 인증 세션은 단일
인스턴스 MVP의 서버 메모리 `HttpSession`으로 유지한다.

**테스트**: JUnit 5, Mockito, MockMvc, Spring Security Test, `@WebMvcTest`,
`@DataJpaTest`, `@SpringBootTest`, Testcontainers MySQL

**대상 플랫폼**: Java 17을 실행하는 단일 Linux 서버 인스턴스와 MySQL 서버

**프로젝트 유형**: 백엔드 REST 웹 서비스

**성능 목표**: 정상 이용 환경에서 크루 및 크루원 목록 결과 또는 실패 안내를 3초 이내에
제공한다. 목록은 기본 20개, 최대 100개 단위로 제한한다.

**제약 조건**: 모든 응답은 정확히 `status`, `message`, `data`, `timestamp`를 가진다.
GET 성공만 `data`에 조회 결과를 담고 변경 성공과 오류는 null을 사용한다. 타임스탬프는
`Asia/Seoul` 기준의 `YYYY-MM-DDThh:mm:ss`이다. 사용자 및 크루 비밀번호 원문을 저장·로그·
응답하지 않는다. 크루당 최대 100명, 사용자-크루 관계 중복 금지, 크루당 관리자 정확히 한
명을 원자적으로 보장한다.

**규모/범위**: 비즈니스 REST 작업 14개와 CSRF 토큰 조회 지원 작업 1개, 핵심 엔티티 3개,
도메인 모듈 4개다. 한 사용자는 여러 크루에 속할 수 있고 한 크루는 최대 100명을 수용한다.
전체 사용자·크루 수는 아직 정해지지 않았으므로 모든 목록을 페이지로 제한하고 조회 및 제약
열에 인덱스를 둔다.

## 헌법 준수 확인

*게이트: 0단계 조사 전에 확인하고 1단계 설계 후 다시 확인한다.*

| 헌법 규칙 | 설계 대응 | 사전 판정 |
|-----------|-----------|-----------|
| 도메인 규칙의 권위 | 문서의 길이·범위·고유성·역할·소속 불변 조건을 검증과 DB 제약으로 이중 보호한다. | 통과 |
| 계층 경계 준수 | 컨트롤러→서비스→도메인/리포지토리 흐름과 도메인별 패키지를 유지한다. | 통과 |
| 일관된 API 응답 계약 | 공통 응답, 중앙 예외 처리, 보안 성공·실패 처리기와 계약 테스트를 설계한다. | 통과 |
| 보안과 소속 무결성 | 단방향 비밀번호 인코딩, 세션·CSRF 보호, 역할 확인, 원자적 위임·삭제를 적용한다. | 통과 |
| 테스트 증명 | 단위·MVC·JPA·실제 MySQL 통합 테스트로 성공, 오류, 경계, 경합을 검증한다. | 통과 |
| 기술 및 문서 제약 | 지정된 Spring Boot/JPA/MySQL/Security/REST를 사용하고 산출물을 한글로 작성한다. | 통과 |

사전 게이트 위반은 없다. 추가 복잡성 추적이 필요한 헌법 예외도 없다.

## 프로젝트 구조

### 이 기능의 문서

```text
specs/001-mvp-detailed-spec/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api-rules.md
├── checklists/
│   └── requirements.md
└── tasks.md                 # $speckit-tasks 단계에서 생성
```

### 소스 코드

```text
src/main/java/com/example/workoutcrew/
├── WorkoutcrewApplication.java
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   └── dto/
├── auth/
│   ├── controller/
│   ├── service/
│   └── dto/
├── crew/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   └── dto/
└── global/
    ├── config/
    ├── security/
    ├── exception/
    └── response/

src/main/resources/
├── application.properties
└── db/migration/

src/test/java/com/example/workoutcrew/
├── user/
├── auth/
├── crew/
└── global/
```

**구조 결정**: 기존 단일 프로젝트와 도메인별 계층 구조를 그대로 사용한다. 인증 흐름은
`auth`, 사용자 상태는 `user`, 크루와 소속 관계는 `crew`가 담당한다. 여러 도메인이 공유하는
보안 설정, 예외 변환, 공통 응답만 `global`에 둔다. 별도 애플리케이션이나 공통 기능만을 위한
추상 모듈은 만들지 않는다.

## 0단계: 조사 결과

[research.md](./research.md)에 다음 결정을 근거 및 대안과 함께 기록했다.

1. Java 17과 Spring Boot 4.0.8 기반 단일 서비스 유지
2. `HttpSession` 인증과 현재 기기만 로그아웃
3. 회원탈퇴 커밋 후 모든 사용자 세션 만료
4. 비밀번호 단방향 인코딩과 CSRF 보호
5. 크루 행 비관적 잠금 및 일관된 다중 행 잠금 순서
6. 데이터베이스 고유·CHECK·외래 키·조건부 관리자 제약
7. 관리자 위임과 회원·크루 탈퇴의 원자적 처리
8. 14개 비즈니스 REST 계약, CSRF 지원 계약과 페이지 기본값
9. 오류 상태 및 `Asia/Seoul` 시간 기준
10. 계층별 테스트와 실제 MySQL 동시성 검증

모든 기술적 확인 사항이 해소되었으며 미해결 확인 항목은 없다.

## 1단계: 설계 결과

### 데이터 모델

[data-model.md](./data-model.md)는 `User`, `Crew`, `CrewUser`의 필드, 검증, 관계, 고유 및
외래 키, 관리자 조건부 고유 인덱스와 다음 상태 전이를 정의한다.

- 회원가입과 회원탈퇴
- 크루 생성, 수정, 삭제
- 일반 크루원 가입, 탈퇴, 추방
- 관리자 위임
- 관리자의 크루 탈퇴와 회원탈퇴에 따른 크루 삭제

### 외부 계약

[api-rules.md](./contracts/api-rules.md)는 14개 비즈니스 REST 작업과 CSRF 토큰 조회 지원
작업의 메서드·경로·요청 필드·성공 메시지·오류 매핑·권한표·목록 및 수정 규칙을 정의한다.

### 검증 가이드

[quickstart.md](./quickstart.md)는 환경 준비, 애플리케이션 및 테스트 실행, 회원·인증·크루
전체 흐름, 경계값, 권한, 동시성, 공통 응답 및 Markdown API 계약 검증 절차를 제공한다.

## 설계 후 헌법 재확인

| 헌법 규칙 | 설계 산출물 검증 | 사후 판정 |
|-----------|------------------|-----------|
| 도메인 규칙의 권위 | 데이터 모델에 모든 기존 필드와 제약을 유지하고 새 비즈니스 속성을 추가하지 않았다. | 통과 |
| 계층 경계 준수 | 계획의 실제 패키지 트리가 기존 아키텍처의 책임과 의존 방향을 유지한다. | 통과 |
| 일관된 API 응답 계약 | Markdown API 계약의 모든 응답이 네 필드 구조와 메서드별 data 규칙을 따른다. | 통과 |
| 보안과 소속 무결성 | 세션, CSRF, 비밀번호 인코딩, 권한표, 잠금·트랜잭션·DB 제약이 설계되었다. | 통과 |
| 테스트 증명 | 빠른 계층별 테스트와 실제 MySQL 경합 및 전체 흐름 검증 절차가 정의되었다. | 통과 |
| 한글 문서화 | 계획, 조사, 데이터 모델, 계약 설명 및 검증 가이드를 한글로 작성했다. | 통과 |

설계 후에도 헌법 위반이나 정당화가 필요한 복잡성은 없다. 다음 단계는
`$speckit-tasks`로 구현 작업을 의존성 순서에 따라 분해하는 것이다.
