# 아키텍처 초안

## Architecture Style
- 단일 Spring Boot 애플리케이션으로 구성한다.
- 기능은 도메인별 패키지로 분리한다.
- 각 도메인 내부에서는 Layered Architecture를 사용한다.

## Main Modules
- user
- auth
- crew
- global

## Package Structure

workoutcrew
├── user
│   ├── controller
│   ├── service
│   ├── repository
│   ├── domain
│   └── dto
│
├── auth
│   ├── controller
│   ├── service
│   └── dto
│
├── crew
│   ├── controller
│   ├── service
│   ├── repository
│   ├── domain
│   └── dto
│
└── global
├── config
├── security
└── exception

## Layer Responsibilities

### Controller
- HTTP 요청과 응답 처리
- Request DTO의 구조적 검증
- Service 호출

### Service
- 비즈니스 유스케이스 수행
- 데이터 조회가 필요한 검증을 포함한 비즈니스 검증 순서 조율
- 여러 Domain/Repository의 흐름 조합
- 트랜잭션 경계

### Domain
- 핵심 상태와 비즈니스 규칙 관리

### Repository
- 데이터 조회 및 저장

## Dependency Direction

Controller
↓
Service
↓
Domain / Repository

## Persistence
- Spring Data JPA
- MySQL

## Security
- Spring Security

## API
- REST API
