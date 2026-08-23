# WorkoutCrew MVP 실행 및 검증 가이드

## 1. 목적

이 문서는 구현 완료 후 [기능 명세](./spec.md), [데이터 모델](./data-model.md),
[API 세부 규칙](./contracts/api-rules.md)이 실제 동작과 일치하는지 검증하는 절차다. 전체
구현 코드나 테스트 코드는 포함하지 않는다.

## 2. 사전 준비

- Java 17
- MySQL 8.x
- 프로젝트의 Gradle Wrapper
- HTTP 호출용 `curl`
- JSON 응답 확인용 `jq`
- 통합 테스트 실행 환경의 Docker 또는 호환 컨테이너 런타임

MySQL에 빈 데이터베이스와 전용 사용자를 준비한 뒤 다음 환경 변수를 현재 셸에 설정한다.

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/workoutcrew'
export SPRING_DATASOURCE_USERNAME='workoutcrew'
export SPRING_DATASOURCE_PASSWORD='로컬_비밀번호'
export SESSION_COOKIE_SECURE='false'
```

운영 비밀번호를 저장소의 `.properties`, Markdown, 테스트 소스에 기록하지 않는다.
`SESSION_COOKIE_SECURE=false`는 HTTP로 실행하는 로컬 검증에만 사용한다. HTTPS 운영 환경은
기본값 `true`를 유지해야 한다.

## 3. 자동화 검증

전체 테스트를 실행한다.

```bash
./gradlew test
```

실제 MySQL이 필요한 테스트만 선택해서 확인하려면 다음과 같이 실행할 수 있다.

```bash
./gradlew test --tests '*IntegrationTest' --tests '*ConcurrencyIntegrationTest'
```

제한된 실행 환경에서 기본 Gradle 캐시를 쓸 수 없다면 작업 전용 캐시 경로를 지정한다.

```bash
GRADLE_USER_HOME=/tmp/workoutcrew-gradle ./gradlew test
```

테스트는 다음 계층을 모두 포함해야 한다.

1. 도메인 및 서비스 단위 테스트
2. MVC 요청·응답·보안 계약 테스트
3. JPA 제약과 쿼리 테스트
4. Testcontainers MySQL 기반 전체 통합 및 동시성 테스트

필수 자동화 검증 항목은 다음과 같다.

- 원본 요구사항의 14개 비즈니스 기능마다 성공 시나리오와 대표 실패 시나리오
- 모든 입력 최솟값·최댓값 성공과 범위 바로 밖의 값 실패
- 이메일·닉네임·크루 이름·소속 관계 고유성
- 마지막 한 자리 동시 가입 시 최대 인원 미초과
- 동시 관리자 위임 후 관리자 정확히 한 명
- 관리자 탈퇴와 회원탈퇴의 전체 성공 또는 전체 롤백
- 세션 A 로그아웃 후 세션 A는 401, 세션 B는 계속 인증
- 회원탈퇴 후 모든 기기 세션 401 및 재로그인 실패
- CSRF 토큰 누락 또는 불일치 상태 변경 요청 403
- 응답 최상위 필드가 정확히 `status`, `message`, `data`, `timestamp`
- GET 성공의 `data`는 null이 아니고 POST·PATCH·DELETE 성공과 모든 오류의 `data`는 null
- 타임스탬프가 서울 시간대의 `YYYY-MM-DDThh:mm:ss` 형식

## 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 주소는 `http://localhost:8080`이다. 다음 요청으로 서버와 CSRF 지원 계약을 확인한다.

브라우저에서 `http://localhost:8080`을 열면 백엔드의 14개 비즈니스 기능을 직접 호출할 수
있는 검증 콘솔이 표시된다. 회원가입·로그인, 크루 생성·가입·관리, 닉네임 변경·회원탈퇴와
원본 공통 응답 로그를 한 화면에서 확인할 수 있다. 로컬 HTTP 환경에서는 앞에서 설명한
`SESSION_COOKIE_SECURE=false` 설정이 필요하다.

```bash
curl -i -c alice.cookies http://localhost:8080/api/v1/auth/csrf
```

기대 결과:

- HTTP 200
- `data`에 `headerName`, `parameterName`, `token` 존재
- 최상위 필드는 공통 응답 네 필드만 존재
- `XSRF-TOKEN` 및 세션 관련 쿠키가 필요한 경우 쿠키 파일에 저장

아래 예시는 조회 응답의 토큰을 셸 변수에 저장한다.

```bash
ALICE_CSRF=$(curl -s -b alice.cookies -c alice.cookies \
  http://localhost:8080/api/v1/auth/csrf | jq -r '.data.token')
```

## 5. 회원가입과 로그인 검증

```bash
curl -i -b alice.cookies -c alice.cookies \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -d '{"email":"alice@example.com","password":"password123","nickname":"앨리스"}' \
  http://localhost:8080/api/v1/users

curl -i -b alice.cookies -c alice.cookies \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -d '{"email":"alice@example.com","password":"password123"}' \
  http://localhost:8080/api/v1/auth/login
```

로그인 성공 후 보안 컨텍스트와 CSRF 토큰이 회전할 수 있으므로 토큰을 다시 조회한다.

```bash
ALICE_CSRF=$(curl -s -b alice.cookies -c alice.cookies \
  http://localhost:8080/api/v1/auth/csrf | jq -r '.data.token')
```

기대 결과:

- 회원가입은 201과 `Location` 헤더, 로그인은 200
- 두 응답 모두 `data: null`
- 중복 이메일 또는 닉네임은 409이며 기존 사용자는 변경되지 않음
- 잘못된 로그인 정보는 이메일 존재 여부와 관계없이 같은 401 메시지

## 6. 크루 흐름 검증

### 6.1 크루 생성과 조회

```bash
curl -i -b alice.cookies \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -d '{"name":"아침운동","password":"crew12","maxUsers":2,"weeklyCertificationGoal":3}' \
  http://localhost:8080/api/v1/crews

curl -s -b alice.cookies \
  'http://localhost:8080/api/v1/crews?page=0&size=20&sort=id,desc' | jq
```

생성 응답은 201과 `Location`, `data: null`이어야 한다. 목록에는 비밀번호 없이 `id`,
`name`, `maxUsers`, `currentUsers`, `weeklyCertificationGoal`만 있어야 한다. 이후 예시에서는
목록에서 확인한 식별자를 사용한다.

```bash
CREW_ID=1
```

### 6.2 두 번째 사용자 가입

Bob용 쿠키 파일로 CSRF 조회, 회원가입, 로그인을 반복한 뒤 다음 요청을 실행한다.

```bash
BOB_CSRF=$(curl -s -b bob.cookies -c bob.cookies \
  http://localhost:8080/api/v1/auth/csrf | jq -r '.data.token')

curl -i -b bob.cookies \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $BOB_CSRF" \
  -d '{"password":"crew12"}' \
  "http://localhost:8080/api/v1/crews/$CREW_ID/members"
```

기대 결과는 201과 `data: null`이다. 같은 요청 반복, 정원 초과, 잘못된 비밀번호는 각각
정의된 409 또는 403으로 실패해야 하며 기존 소속과 정원은 유지되어야 한다.

### 6.3 크루원 조회와 관리자 위임

```bash
curl -s -b alice.cookies \
  "http://localhost:8080/api/v1/crews/$CREW_ID/members?page=0&size=20&sort=id,desc" | jq

BOB_USER_ID=2

curl -i -b alice.cookies \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -X PATCH \
  -d "{\"targetUserId\":$BOB_USER_ID}" \
  "http://localhost:8080/api/v1/crews/$CREW_ID/manager"
```

위임 전후 크루원 목록에는 `MANAGER`가 정확히 한 명 있어야 한다. 위임 후 Alice의 관리
요청은 403이고 Bob의 관리 요청은 성공해야 한다.

## 7. 기기별 로그아웃과 회원탈퇴 검증

같은 Alice 계정으로 별도 `alice-device-b.cookies` 파일에 로그인한다. 기기 A에서 로그아웃한
후 A와 B에서 보호된 목록을 각각 호출한다.

```bash
curl -i -b alice.cookies \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -X POST http://localhost:8080/api/v1/auth/logout

curl -i -b alice.cookies http://localhost:8080/api/v1/crews
curl -i -b alice-device-b.cookies http://localhost:8080/api/v1/crews
```

기기 A는 401, 기기 B는 200이어야 한다. 회원탈퇴를 별도 사용자로 검증할 때는 탈퇴 완료 후
모든 기기가 401이고 같은 자격 증명으로 재로그인할 수 없어야 한다. 탈퇴 사용자가 관리하던
크루는 모두 사라지고 일반 크루원으로 있던 관계만 제거되어야 한다.

## 8. 공통 응답 계약 검증

대표 성공 및 오류 응답을 `jq`로 검사한다.

```bash
curl -s -b alice-device-b.cookies http://localhost:8080/api/v1/crews \
  | jq 'keys, .status, .data, .timestamp'
```

확인 기준:

- `keys` 결과가 `data`, `message`, `status`, `timestamp` 네 개뿐이다.
- `status`가 실제 HTTP 상태와 같다.
- GET 성공은 요청한 데이터 또는 빈 페이지를 `data`에 제공한다.
- POST, PATCH, DELETE 성공과 모든 오류는 `data: null`이다.
- `timestamp`는 초 단위까지 있고 시간대 기준은 `Asia/Seoul`이다.
- 응답과 로그 어디에도 사용자 또는 크루 비밀번호가 나타나지 않는다.

## 9. API 계약 최종 검증

[API 세부 규칙](./contracts/api-rules.md)의 14개 비즈니스 작업과 CSRF 토큰 조회 지원 작업을
MVC 계약 테스트와 위의 수동 흐름으로 대조한다. 각 작업의 메서드·경로·상태·메시지·공통
응답·세션 쿠키·CSRF 헤더·페이지 제한이 문서와 일치해야 한다.

외부 계약의 기준은 이 디렉터리의 한글 Markdown 문서와 자동화된 MVC 계약 테스트다.
