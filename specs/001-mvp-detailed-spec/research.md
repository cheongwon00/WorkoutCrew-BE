# WorkoutCrew MVP 기술 조사

## 1. 애플리케이션 구조와 버전

**결정**: Java 17과 Spring Boot 4.0.8을 유지하고, 단일 REST 웹 서비스 안에서 `user`,
`auth`, `crew`, `global` 도메인별 패키지와 계층형 구조를 사용한다.

**근거**: 현재 빌드와 아키텍처 문서가 이미 이 조합을 지정한다. Spring Boot 4.0은 Java 17
이상을 요구하므로 현재 도구 모음과 호환된다. 기존 선택을 유지하는 것이 MVP 범위와 헌법의
단순성·계층 경계 원칙을 가장 잘 지킨다.

**검토한 대안**: 멀티 모듈 또는 마이크로서비스 분리는 배포와 트랜잭션 복잡성만 늘리고 현재
도메인 규모에 필요한 독립성을 제공하지 않아 제외했다.

**공식 근거**: [Spring Boot 설치 요구사항](https://docs.spring.io/spring-boot/4.0/installing.html)

## 2. 인증 상태와 기기별 로그아웃

**결정**: 서버 측 `HttpSession`과 세션 쿠키로 인증 상태를 유지한다. 로그인 성공 시
`SecurityContext`를 세션 저장소에 명시적으로 저장하고, 동시 세션 수는 제한하지 않는다.
로그아웃은 Spring Security의 표준 로그아웃 흐름을 사용하여 요청을 보낸 현재 세션만
무효화한다.

**근거**: 각 기기는 별도 세션을 가지므로 여러 기기 로그인을 허용하면서 현재 기기만
로그아웃한다는 확정 요구를 추가 토큰 저장소 없이 충족한다. JSON 로그인 성공·실패 처리기는
리다이렉트 대신 프로젝트 공통 응답을 반환한다. 세션 고정 공격 방지를 위해 로그인 성공 시
세션 ID를 변경한다.

**검토한 대안**: JWT access/refresh 토큰은 현재 기기 로그아웃과 회원탈퇴 시 즉시 폐기를
위해 토큰 회수 저장소와 회전 정책이 필요하다. HTTP Basic은 서버가 종료할 로그인 상태가
없다. 두 방식 모두 현재 MVP에는 불필요한 복잡성을 만든다.

**공식 근거**:
[Spring Security 세션 관리](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html),
[인증 정보 유지](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/persistence.html),
[로그아웃](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html)

## 3. 회원탈퇴와 전체 세션 종료

**결정**: 인메모리 `SessionRegistry`로 사용자별 세션을 추적한다. 회원탈퇴 데이터
트랜잭션이 커밋된 뒤, 삭제 전에 확보한 불변 사용자 식별자로 모든 세션을 만료시키며 완료
응답 전 만료가 끝나야 한다. 일반 로그아웃은 이 전체 만료 절차를 사용하지 않는다.

**근거**: 계정만 삭제하면 다른 기기의 세션에 남은 인증 정보가 즉시 사라지지 않을 수 있다.
커밋 이후 만료하면 데이터 롤백과 인증 만료가 엇갈리는 문제를 피할 수 있다. 현재 목표는
단일 인스턴스이므로 공유 세션 인프라는 필요하지 않다.

**검토한 대안**: 모든 보호 요청마다 사용자 존재 여부를 조회하면 안전하지만 요청마다
데이터베이스 비용이 발생한다. 다중 인스턴스로 확장할 때는 Spring Session JDBC 또는 Redis
기반 공유 세션 저장소로 교체해야 한다.

## 4. 비밀번호와 CSRF 보호

**결정**: 사용자 비밀번호와 크루 가입 비밀번호는 모두 입력 길이를 먼저 검증한 뒤
`DelegatingPasswordEncoder`로 단방향 인코딩한다. 로그인 실패는 이메일 존재 여부와
비밀번호 오류를 같은 외부 메시지로 처리한다. 세션 쿠키 인증이므로 POST, PATCH, DELETE에
CSRF 보호를 유지하고 운영 쿠키에 Secure, HttpOnly, SameSite 정책을 적용한다.

**근거**: 가입용 크루 비밀번호도 접근 자격 증명이므로 원문 저장과 노출을 금지해야 한다.
적응형 단방향 함수와 알고리즘 식별자가 있는 저장 형식은 향후 안전한 업그레이드를 지원한다.
쿠키가 자동 전송되는 인증 방식에서 CSRF를 비활성화하면 상태 변경 요청 위조가 가능하다.

**검토한 대안**: 고정 BCrypt만 사용해도 해시는 가능하지만 알고리즘 전환 유연성이 낮다.
CSRF 비활성화는 보안 헌법과 맞지 않아 제외했다.

**공식 근거**:
[비밀번호 저장](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html),
[CSRF 보호](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html)

## 5. 크루 변경의 동시성 제어

**결정**: 가입·탈퇴·추방, 최대 인원 변경, 관리자 위임, 크루 삭제는 모두 짧은 서비스
트랜잭션으로 처리하며 가장 먼저 대상 `Crew` 행을 비관적 쓰기 잠금으로 조회한다. 다중 행
작업은 사용자 ID, 크루 ID, 소속 ID의 오름차순으로 잠근다. 일시적 교착 또는 잠금 획득
실패는 트랜잭션 바깥에서 제한된 횟수만 재시도한다.

**근거**: 하나의 크루 행을 직렬화 지점으로 사용하면 마지막 정원 가입, 최대 인원 축소,
동시 관리자 위임을 같은 순서로 처리할 수 있다. 빈 크루에서도 잠글 안정적인 부모 행이
존재한다.

**검토한 대안**: 단순 인원 조회는 검사와 삽입 사이 경쟁을 막지 못한다. 애플리케이션 메모리
잠금은 다중 인스턴스에서 무효하다. 전체 SERIALIZABLE 격리는 필요 이상으로 잠금 범위를
넓힌다.

**공식 근거**:
[Spring Data JPA 잠금](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html),
[MySQL 잠금 읽기](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html),
[교착 상태 처리](https://dev.mysql.com/doc/refman/8.4/en/innodb-deadlocks-handling.html)

## 6. 데이터베이스 무결성과 삭제 방식

**결정**: 명시적 마이그레이션으로 이메일, 닉네임, 크루 이름과 `(user_id, crew_id)`에 고유
제약을 둔다. 범위와 역할 값에는 CHECK 제약을, 소속 관계에는 NOT NULL과 외래 키를 둔다.
`MANAGER` 행에만 `crew_id`를 표시하는 보조 열과 고유 인덱스로 크루당 관리자를 최대 한
명으로 제한한다. MySQL의 생성 열과 cascade 외래 키 조합 제한 때문에 실제 마이그레이션은
nullable 보조 열, 역할 연계 CHECK, 역할 전이 시 동시 갱신을 사용한다. 최소 한 명 조건은
생성·위임·삭제 트랜잭션으로 보장한다. 회원 및 크루
삭제는 현재 보존 요구가 없으므로 하드 삭제한다. 크루 삭제는 소속 관계를 외래 키 cascade로
제거하고, 사용자 삭제는 관리 크루와 일반 소속을 서비스가 먼저 정리한 뒤 수행하도록 사용자
외래 키를 삭제 제한으로 둔다.

**근거**: 서비스의 선행 검증은 친절한 오류를 제공하고 데이터베이스 제약은 경합과 우회
쓰기의 최종 방어선이 된다. 하드 삭제는 과거 활동 데이터가 아직 없는 MVP에서 가장 단순하며
삭제 후 활성 조회 불가 요구를 직접 충족한다.

**검토한 대안**: `Crew.managerUserId`는 기존 도메인 모델을 변경하고 역할 상태를 중복한다.
소프트 삭제는 보존 정책 없이 모든 고유 제약과 조회에 활성 조건을 추가한다. 트리거는 위임
중간 상태와 테스트 복잡성을 높인다.

**공식 근거**:
[MySQL 고유·함수형 인덱스](https://dev.mysql.com/doc/refman/8.4/en/create-index.html),
[CHECK 제약](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html),
[외래 키 cascade](https://dev.mysql.com/doc/refman/8.4/en/example-foreign-keys.html)

## 7. 관리자 위임 및 탈퇴 트랜잭션

**결정**: 관리자 위임은 크루를 잠근 후 요청자와 대상을 재검증하고, 기존 관리자를
`MEMBER`로 강등하여 flush한 다음 대상을 `MANAGER`로 승격한다. 어느 단계든 실패하면 전체를
롤백한다. 관리자의 크루 탈퇴는 크루 삭제로 처리한다. 회원탈퇴는 사용자를 잠근 뒤 관리하는
크루들을 ID 순서로 잠가 삭제하고, 일반 소속 관계와 사용자를 한 트랜잭션에서 삭제한다.

**근거**: 조건부 관리자 고유 인덱스가 있을 때 새 관리자를 먼저 승격하면 충돌한다. 기존
관리자를 먼저 강등한 뒤 같은 트랜잭션에서 승격하면 중간 상태가 외부에 노출되지 않고 실패
시 모두 롤백된다. 탈퇴 처리도 부분 삭제 상태를 남기지 않는다.

**검토한 대안**: 단계별 독립 트랜잭션과 비동기 정리는 관리자 없는 크루 또는 일부만 삭제된
상태를 만들 수 있어 제외했다.

**공식 근거**:
[Spring 선언적 트랜잭션](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html)

## 8. REST 계약과 목록 기본값

**결정**: 기본 경로는 `/api/v1`로 한다. 생성은 201과 `Location` 헤더, 나머지 변경은 공통
본문을 유지하기 위해 200을 사용한다. GET만 `data`에 데이터를 담고 POST, PATCH, DELETE와
오류는 `data: null`을 사용한다. 크루 목록은 인증된 사용자에게 제공하고 크루원 목록은 해당
크루 소속자에게만 제공한다. 목록은 `page=0`, `size=20`, `id,desc`, 최대 크기 100을 기본
계약으로 사용하며 빈 목록은 200과 빈 `content`를 반환한다.

**근거**: 생성 자원은 `Location`으로 식별하면서 응답 본문의 null 규칙을 지킬 수 있다.
204는 응답 본문을 가질 수 없어 헌법과 충돌한다. 인증된 사용자의 전체 크루 목록은 가입할
크루를 탐색해야 하는 사용자 스토리를 충족하고, 페이지 상한과 정렬은 응답 크기와 순서를
결정적으로 만든다.

**검토한 대안**: 행위명 URI는 리소스 중심 구조와 일관성이 낮다. 전체 목록 반환은 데이터
증가에 취약하다. 비인증 공개 목록은 요구사항에 없고 크루 정보를 불필요하게 노출한다.

**공식 근거**:
[HTTP 의미론](https://datatracker.ietf.org/doc/html/rfc9110),
[Spring MVC ResponseEntity](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html)

## 9. 공통 오류와 시간 기준

**결정**: 전역 예외 처리기는 검증 오류 400, 미인증 401, 권한 부족 403, 대상 없음 404,
고유성·정원·상태·동시성 충돌 409, 메서드 오류 405, 미디어 타입 오류 415, 예상하지 못한
오류 500으로 변환한다. 모든 오류의 `data`는 null이다. 타임스탬프는 `Asia/Seoul` 기준의
`YYYY-MM-DDThh:mm:ss`로 만들며 주입 가능한 `Clock`으로 테스트한다.

**근거**: 구조적 요청 오류와 현재 상태 충돌을 구분하면 클라이언트의 수정 또는 재시도 판단이
명확해진다. 요구 형식은 오프셋이 없으므로 시간대를 고정해야 같은 문자열의 의미가 환경마다
달라지지 않는다. Spring 기본 `ProblemDetail`은 프로젝트가 허용한 네 필드와 다르므로 직접
공통 응답으로 변환해야 한다.

**검토한 대안**: UTC는 오프셋 없는 문자열에서 클라이언트가 UTC임을 알 수 없다. 204와
`ProblemDetail`은 공통 응답 계약과 충돌한다.

**공식 근거**:
[Spring MVC 오류 응답](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html),
[Spring MVC 검증](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)

## 10. 테스트 및 계약 문서

**결정**: 순수 단위 테스트, MVC 계약 슬라이스, JPA 슬라이스, 실제 MySQL 기반 전체 통합
테스트의 네 단계로 검증한다. MySQL 고유 인덱스와 잠금 동작은 Testcontainers MySQL로
검증한다. 14개 비즈니스 작업과 CSRF 토큰 조회 지원 작업은 Markdown API 계약을 기준으로
MVC 계약 테스트에서 상태, 응답 구조, 세션 쿠키, CSRF 및 페이지 동작을 검증한다.

**근거**: 각 책임을 가장 작은 컨텍스트에서 검증하면서 운영 데이터베이스에 의존하는
동시성 보장은 실제 MySQL로 확인해야 한다. H2만으로는 함수형 인덱스와 InnoDB 잠금 의미를
증명할 수 없다.

**검토한 대안**: 모든 테스트를 전체 컨텍스트로 실행하면 느리고 실패 원인이 넓어진다.
모든 저장소 테스트를 임베디드 DB로 실행하면 운영 DB 차이를 놓친다. 계약 검증 없이 문서만
관리하면 구현과 계약이 쉽게 달라지므로 MVC 계약 테스트를 필수로 둔다.

**공식 근거**:
[Spring Boot 테스트](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html),
[MockMvc](https://docs.spring.io/spring/reference/testing/mockmvc.html),
[Spring Security CSRF 테스트](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/csrf.html)

## 11. 요구사항 개수 정정

**결정**: 계획과 계약은 `docs/requirements.md`에 실제로 열거된 14개 기능을 모두 다룬다.
상세 명세에서 잘못 기재된 13개 표기는 14개로 정정했다.

**근거**: 회원·인증 5개, 크루 4개, 가입·크루원 관리 5개로 합계가 14개다. 이는 기능 추가나
삭제가 아니라 산술 오류 수정이다.

**검토한 대안**: 숫자 13에 맞추기 위해 기능을 합치거나 누락하면 원본 요구사항을 임의로
변경하게 되므로 제외했다.
