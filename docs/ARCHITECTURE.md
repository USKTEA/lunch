# Lunch 프로젝트 아키텍처 문서

> 최종 업데이트: 2025년 1월

## 목차
- [1. 프로젝트 개요](#1-프로젝트-개요)
- [2. 기술 스택 및 의존성](#2-기술-스택-및-의존성)
- [3. 백엔드 아키텍처](#3-백엔드-아키텍처)
- [4. 프론트엔드 아키텍처](#4-프론트엔드-아키텍처)
- [5. 인증/인가 시스템 완전 가이드](#5-인증인가-시스템-완전-가이드)
- [6. H3 지리 공간 인덱싱 시스템](#6-h3-지리-공간-인덱싱-시스템)
- [7. 데이터베이스 스키마](#7-데이터베이스-스키마)
- [8. API 엔드포인트](#8-api-엔드포인트)
- [9. 빌드 및 배포](#9-빌드-및-배포)

---

## 1. 프로젝트 개요

서울시 일반음식점 데이터를 수집하고 관리하는 Spring Boot 기반 점심 추천 애플리케이션입니다.

### 주요 기능
- **인증/인가**: OAuth2 (Azure AD) + JWT 토큰 시스템
  - Access Token + Refresh Token (HttpOnly Cookie)
  - 자동 토큰 갱신 메커니즘
- **데이터 크롤링**: 서울 열린데이터광장 API 연동
  - PostgreSQL CDC를 통한 실시간 데이터 동기화
  - 네이버 지도 API 연동으로 정확한 주소 및 좌표 획득
- **공간 검색**: PostGIS + H3 지리 공간 인덱싱
  - 지도 영역 기반 식당 조회
  - 줌 레벨별 최적화된 쿼리
- **리뷰 시스템**: 식당 평가 및 리뷰 작성
  - Kotlin JDSL 기반 동적 쿼리
  - 커서 기반 페이징
  - S3 Presigned URL을 통한 이미지 업로드
  - 평점 통계 집계 (CASE WHEN 활용)

---

## 2. 기술 스택 및 의존성

### 2.1 백엔드 (Kotlin + Spring Boot)

#### 핵심 프레임워크
- **언어**: Kotlin 2.0.21
- **프레임워크**: Spring Boot 3.3.5
- **빌드 도구**: Gradle (Kotlin DSL)
- **Java 버전**: 21

#### 주요 Spring 의존성
```kotlin
// Spring Boot Starters
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")        // OAuth2 클라이언트
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT 검증

// Spring Cloud
implementation("org.springframework.cloud:spring-cloud-starter-openfeign")  // 외부 API 호출

// 데이터베이스
implementation("org.postgresql:postgresql")
implementation("org.hibernate.orm:hibernate-spatial")  // PostGIS 지원
implementation("org.locationtech.jts:jts-core:1.19.0")  // 공간 데이터 처리

// Kotlin JDSL - 동적 쿼리 DSL
implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.6.1")
implementation("com.linecorp.kotlin-jdsl:jpql-render:3.6.1")
implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.6.1")

// JWT
implementation("com.nimbusds:nimbus-jose-jwt:10.5")

// 기타
implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.11.0")
implementation("com.uber:h3:4.3.1")  // 지리 공간 인덱싱
```

#### 설정 파일 위치
- **application.yml**: `src/main/resources/application-local.yml`
- **주요 설정 항목**:
  - PostgreSQL 연결 정보
  - OAuth2 클라이언트 설정 (Azure AD)
  - JWT 키 및 만료 시간
  - CORS 설정

### 2.2 프론트엔드 (React)

#### 핵심 라이브러리
```json
{
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^7.9.3",
    "axios": "^1.7.3",
    "styled-components": "^6.1.19",
    "@toss/react": "^1.8.1"
  }
}
```

#### 상태 관리
- **Custom Store Pattern** (Observer Pattern 기반)
- Redux, MobX, Zustand 등 외부 라이브러리 사용 안함

#### 빌드
- **Create React App** (react-scripts 5.0.1)
- **빌드 출력**: `web/build` → `src/main/resources/static` (Gradle 통합)

### 2.3 데이터베이스
- **PostgreSQL**: 메인 데이터베이스
- **PostGIS Extension**: 공간 데이터 저장 및 검색
- **CDC (Change Data Capture)**: wal2json 플러그인 사용

---

## 3. 백엔드 아키텍처

### 3.1 프로젝트 구조

```
src/main/kotlin/com/usktea/lunch/
├── client/                         # 외부 API 클라이언트
│   ├── NaverMapApiClient.kt       # 네이버 지도 API
│   ├── SeoulOpenDataClient.kt     # 서울 열린데이터광장 API
│   ├── config/
│   └── vo/
│
├── cdc/                           # CDC 관련
│   ├── SeoulRestaurantV2ChangeEvent.kt
│   └── Wal2JsonV2Dto.kt
│
├── common/                        # 공통 VO 및 Enum
│   ├── AuthorizationGrantType.kt  # OAuth2 Grant Type
│   ├── TokenType.kt               # Bearer
│   └── TokenVo.kt                 # 토큰 정보
│
├── config/                        # 애플리케이션 설정
│   ├── SecurityConfig.kt          # Spring Security 메인 설정
│   ├── WebMvcConfig.kt            # CORS, Resource Handler
│   ├── FeignConfig.kt
│   ├── ObjectMapperConfig.kt
│   │
│   ├── auth/                      # 인증/인가 관련 설정
│   │   ├── OAuth2LoginSuccessHandler.kt         # OAuth2 로그인 성공 핸들러
│   │   ├── CustomJwtAuthenticationConverter.kt  # JWT → Authentication 변환
│   │   ├── CustomAuthenticationToken.kt         # 커스텀 인증 토큰 (userId 보유)
│   │   └── CustomAuthenticationEntryPoint.kt    # 401 처리 (Provider 정보 반환)
│   │
│   └── web/
│       ├── CorsProperties.kt      # CORS 설정 Properties
│       └── TokenRequestResolver.kt # IssueTokenRequest 파라미터 리졸버
│
├── controller/                    # REST 컨트롤러
│   ├── TokenController.kt         # 토큰 발급 (POST /api/auth/tokens)
│   ├── RestaurantController.kt    # 레스토랑 검색 (GET /api/restaurants)
│   └── WebPageController.kt
│   └── vo/
│       ├── IssueTokenRequest.kt   # Sealed Interface (AuthorizationCode, RefreshToken)
│       └── IssueTokenResponse.kt
│
├── entity/                        # JPA 엔티티
│   ├── UserEntity.kt              # 사용자 (lunch.users)
│   ├── UserIdentityEntity.kt      # 외부 인증 연동 정보 (lunch.user_identity)
│   ├── AuthorizationSessionEntity.kt  # OAuth2 인증 세션 (lunch.authorization_session)
│   ├── TokenEntity.kt             # Refresh Token 관리 (lunch.token)
│   ├── RestaurantEntity.kt        # 최종 레스토랑 정보 (lunch.restaurant)
│   ├── SeoulRestaurantEntity.kt   # 서울시 원천 데이터 (open_data_cloud.seoul_restaurant)
│   └── common/
│       └── AuditingBaseEntity.kt  # 생성/수정 시간 자동 관리
│
├── listener/                      # CDC 이벤트 리스너
│   ├── SeoulRestaurantCdcListener.kt
│   └── Wal2JsonListener.kt
│
├── repository/                    # JPA 리포지토리
│
├── service/
│   ├── api/                       # API 비즈니스 로직
│   │   ├── TokenApiService.kt     # 토큰 발급 및 갱신 로직
│   │   └── RestaurantApiService.kt
│   │
│   ├── auth/                      # 인증 관련 서비스
│   │   └── AuthorizationSessionAuthService.kt  # OAuth2 세션 생성
│   │
│   ├── crawler/                   # 데이터 크롤러
│   │
│   ├── entity/                    # 엔티티 서비스 (CRUD)
│   │   ├── UserEntityService.kt
│   │   ├── TokenEntityService.kt
│   │   ├── AuthorizationRequestEntityService.kt   # OAuth2 요청 저장 (DB 기반 Repository)
│   │   └── AuthorizationSessionEntityService.kt
│   │
│   └── event/                     # 이벤트 처리
│
├── util/
│   └── JwtUtil.kt                 # JWT 생성 및 검증
│
└── LunchApplication.kt            # 메인 애플리케이션
```

### 3.2 레이어별 역할

#### Controller Layer
- HTTP 요청/응답 처리
- **TokenController**: 토큰 발급 및 갱신 엔드포인트
- **RestaurantController**: 레스토랑 검색 API (인증 필요)

#### Service Layer
- **API Service**: 비즈니스 로직 처리
- **Auth Service**: 인증 관련 로직
- **Entity Service**: 엔티티 CRUD 로직
- **Crawler Service**: 외부 데이터 수집

#### Repository Layer
- JPA 기반 데이터 접근
- JDBC Template 직접 사용 (AuthorizationRequestEntityService)

#### Config Layer
- **SecurityConfig**: Spring Security 설정 (OAuth2 + JWT)
- **WebMvcConfig**: CORS, Argument Resolver 등록

---

## 4. 프론트엔드 아키텍처

### 4.1 프로젝트 구조

```
web/src/
├── component/              # 재사용 가능한 UI 컴포넌트
│
├── hooks/                  # 커스텀 React Hooks
│   ├── useStore.js        # Store 구독 Hook
│   └── useForceUpdate.js  # 강제 리렌더링 Hook
│
├── pages/                  # 페이지 컴포넌트
│   ├── LoginPage.jsx      # 로그인 페이지 (OAuth2 Provider 선택 + Code 교환)
│   └── MainPage.jsx
│
├── service/                # API 통신 레이어
│   └── ApiService.js      # Axios 기반 HTTP 클라이언트
│
├── stores/                 # 상태 관리 Store 클래스
│   ├── Store.js           # Base Store 클래스 (Observer Pattern)
│   └── AuthStore.js       # 인증 상태 관리 Store
│
├── App.jsx                 # 루트 컴포넌트 (라우팅)
└── index.js                # 애플리케이션 진입점
```

### 4.2 상태 관리 패턴 (Custom Store)

> **중요**: 이 프로젝트는 **Observer Pattern 기반 커스텀 Store**를 사용합니다.

#### Store.js (Base Class)
```javascript
class Store {
  listeners = new Set();

  subscribe(callback) {
    this.listeners.add(callback);
    return () => this.listeners.delete(callback);  // unsubscribe
  }

  publish() {
    this.listeners.forEach(callback => callback());
  }
}
```

#### AuthStore.js (Authentication State)
```javascript
class AuthStore extends Store {
  tokenInfo = null;           // { accessToken, issuedAt, tokenType, expiresIn, expiresAt }
  providers = [];             // OAuth2 Provider 목록 (서버에서 받음)
  redirectAfterLogin = null;  // 로그인 후 리다이렉트 경로
  navigate = null;            // React Router의 navigate 함수

  // 로컬스토리지에 토큰 저장
  setTokenInfo(tokenData) { ... }
  getAccessToken() { ... }
  isTokenExpired() { ... }
  clearToken() { ... }

  // OAuth2 플로우
  startWith(provider, redirectUri) { ... }  // OAuth2 인증 시작
  getToken(code, grantType, redirectUri) { ... }  // Code → Token 교환

  // 401 처리
  requireLogin(errorData) { ... }
}
```

#### useStore Hook
```javascript
function useStore(store) {
  const [, forceUpdate] = useForceUpdate();

  useEffect(() => {
    const unsubscribe = store.subscribe(forceUpdate);
    return unsubscribe;
  }, [store]);

  return store;
}
```

### 4.3 ApiService (HTTP 클라이언트)

#### Axios Interceptor
- **Request Interceptor**: `Authorization: Bearer {accessToken}` 헤더 자동 추가
- **Response Interceptor**:
  - 401 응답 시 Refresh Token으로 재발급 시도
  - 재발급 실패 시 로그인 페이지로 리다이렉트

```javascript
// src/service/ApiService.js:16-28
this.instance.interceptors.request.use((config) => {
  const token = authStore.getAccessToken();
  const tokenType = authStore.getTokenType();

  if (token) {
    config.headers.Authorization = `${tokenType} ${token}`;
  }

  return config;
});
```

```javascript
// src/service/ApiService.js:31-98
this.instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // 1. Refresh Token으로 재발급 시도
      // 2. 실패 시 authStore.requireLogin(errorData) 호출
    }
  }
);
```

---

## 5. 인증/인가 시스템 완전 가이드

> **이 섹션은 프로젝트에서 가장 중요한 부분입니다.**
> "프론트가 토큰을 발급받고 Authorization 헤더에 토큰을 주었을 때 서버는 어디에서 토큰을 확인하고 인증을 통과시키는지" 같은 모든 질문에 대한 답이 여기에 있습니다.

### 5.1 인증 아키텍처 개요

이 프로젝트는 **OAuth2 Authorization Code Grant + JWT 토큰** 방식을 사용합니다.

```
[프론트엔드] ← OAuth2 + JWT → [백엔드] ← OAuth2 → [Azure AD]
```

#### 주요 컴포넌트
1. **OAuth2 Client** (Spring Security): Azure AD와 OAuth2 인증 수행
2. **OAuth2 Resource Server** (Spring Security): JWT 토큰 검증
3. **Custom Authorization Session**: OAuth2 인증 결과를 임시 저장
4. **JWT Util**: Access Token 및 Refresh Token 생성
5. **AuthStore** (프론트엔드): 토큰 관리 및 자동 갱신

### 5.2 전체 인증 플로우 (시퀀스 다이어그램)

```
사용자              프론트엔드                백엔드                  Azure AD
  |                   |                       |                         |
  |  1. /web/login   |                       |                         |
  |----------------->|                       |                         |
  |                  |                       |                         |
  |  2. OAuth2 Provider 선택 (Azure)         |                         |
  |<-----------------|                       |                         |
  |                  |                       |                         |
  |  3. 로그인 버튼 클릭                        |                         |
  |----------------->|                       |                         |
  |                  |  4. OAuth2 인증 시작   |                         |
  |                  | (state, redirect_uri) |                         |
  |                  |---------------------->|                         |
  |                  |                       |  5. Azure로 리다이렉트    |
  |                  |                       |------------------------>|
  |                  |                       |                         |
  |  6. Azure 로그인 화면                      |                         |
  |<--------------------------------------------------------------|
  |                  |                       |                         |
  |  7. 로그인 성공 (email, name, sub)        |                         |
  |---------------------------------------------------------------->|
  |                  |                       |                         |
  |                  |                       |  8. OAuth2 Callback      |
  |                  |                       |   (code, state)         |
  |                  |                       |<------------------------|
  |                  |                       |                         |
  |                  |                       |  9. AuthorizationSessionEntity 생성
  |                  |                       |   (provider, subject, userProfile)
  |                  |                       |   DB 저장               |
  |                  |                       |                         |
  |                  |  10. 프론트로 리다이렉트 |                         |
  |                  |   (/web/login?code=xxx)|                        |
  |                  |<----------------------|                         |
  |                  |                       |                         |
  |  11. code 파라미터 감지                    |                         |
  |<-----------------|                       |                         |
  |                  |                       |                         |
  |                  |  12. POST /api/auth/tokens                      |
  |                  |   (code, state, redirect_uri, grant_type)       |
  |                  |---------------------->|                         |
  |                  |                       |  13. AuthorizationSession 검증
  |                  |                       |   (code, state, redirect_uri 일치)
  |                  |                       |                         |
  |                  |                       |  14. User 조회 또는 생성  |
  |                  |                       |   (provider + subject)  |
  |                  |                       |                         |
  |                  |                       |  15. JWT 생성            |
  |                  |                       |   (JwtUtil.generate)    |
  |                  |                       |   - Access Token (1시간) |
  |                  |                       |   - Refresh Token (180일)|
  |                  |                       |                         |
  |                  |                       |  16. TokenEntity 저장    |
  |                  |                       |   (familyId, userId, refreshToken)
  |                  |                       |                         |
  |                  |  17. 토큰 응답          |                         |
  |                  |   { accessToken, expiresIn, expiresAt }        |
  |                  |   Set-Cookie: refresh_token (HttpOnly)         |
  |                  |<----------------------|                         |
  |                  |                       |                         |
  |  18. 로그인 성공 → /web/main 이동           |                         |
  |<-----------------|                       |                         |
  |                  |                       |                         |
  |  19. GET /api/restaurants                |                         |
  |----------------->|  20. Authorization: Bearer {accessToken}        |
  |                  |---------------------->|                         |
  |                  |                       |  21. JWT 검증            |
  |                  |                       |   (JwtDecoder.decode)   |
  |                  |                       |                         |
  |                  |                       |  22. CustomJwtAuthenticationConverter
  |                  |                       |   → CustomAuthenticationToken (userId)
  |                  |                       |                         |
  |                  |                       |  23. SecurityContext에 저장
  |                  |                       |   → 인증 완료           |
  |                  |                       |                         |
  |                  |  24. 레스토랑 데이터 응답 |                         |
  |                  |<----------------------|                         |
  |                  |                       |                         |
  |  25. 화면 렌더링  |                       |                         |
  |<-----------------|                       |                         |
```

### 5.3 백엔드: Spring Security 설정 (SecurityConfig.kt)

> **위치**: `src/main/kotlin/com/usktea/lunch/config/SecurityConfig.kt:23-49`

```kotlin
@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        // 1. 인가 설정
        .authorizeHttpRequests { auth ->
            auth.requestMatchers("/oauth2/**", "/api/auth/tokens").permitAll()  // 인증 불필요
            auth.anyRequest().authenticated()  // 나머지 모든 요청은 인증 필요
        }

        // 2. OAuth2 로그인 설정 (Azure AD)
        .oauth2Login { oAuth2 ->
            oAuth2.authorizationEndpoint { endPoint ->
                // OAuth2 요청을 DB에 저장 (JDBC 기반)
                endPoint.authorizationRequestRepository(authorizationRequestEntityService)
            }.successHandler(oAuth2LoginSuccessHandler)  // 로그인 성공 시 처리
        }

        // 3. OAuth2 Resource Server 설정 (JWT 검증)
        .oauth2ResourceServer { oAuth2 ->
            oAuth2.jwt { jwt ->
                // JWT → CustomAuthenticationToken 변환
                jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
            }
        }

        // 4. 401 예외 처리
        .exceptionHandling { exception ->
            exception.authenticationEntryPoint(customAuthenticationEntryPoint)
        }

        // 5. Stateless 세션 (JWT 사용)
        .sessionManagement { sessionManager ->
            sessionManager.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }

        .csrf { csrf -> csrf.disable() }
        .cors { }

    return http.build()
}
```

#### 핵심 포인트
1. **OAuth2 로그인**: `/oauth2/authorization/azure`로 시작하여 Azure AD 인증 수행
2. **JWT 검증**: `Authorization: Bearer {token}` 헤더로 들어온 요청을 자동으로 검증
3. **Stateless**: 세션을 사용하지 않음 (JWT 기반)

### 5.4 OAuth2 로그인 플로우 상세

#### Step 1: 프론트엔드에서 OAuth2 시작

> **위치**: `web/src/stores/AuthStore.js:15-21`

```javascript
startWith(provider, redirectUri) {
  console.log('Starting OAuth2 login with:', provider);

  const state = crypto.randomUUID();  // CSRF 방지용 state 생성
  sessionStorage.setItem('state', state);

  // Spring Security OAuth2 엔드포인트로 리다이렉트
  window.location.href = `http://localhost:8080${provider.authorizationUri}?redirect_uri=${redirectUri}&state=${state}`;
}
```

#### Step 2: Spring Security OAuth2 처리

Spring Security가 자동으로 다음을 수행합니다:
1. `AuthorizationRequestRepository`를 통해 요청 정보를 DB에 저장
2. Azure AD로 리다이렉트 (`/authorize` 엔드포인트)
3. Azure 로그인 후 callback 처리 (`/login/oauth2/code/azure`)

#### Step 3: OAuth2 로그인 성공 핸들러

> **위치**: `src/main/kotlin/com/usktea/lunch/config/auth/OAuth2LoginSuccessHandler.kt:17-26`

```kotlin
override fun onAuthenticationSuccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication,
) {
    // Azure에서 받은 사용자 정보(email, name, sub)로 AuthorizationSession 생성
    val authorizationSession = authorizationSessionAuthService.createSession(authentication)

    // 프론트엔드로 리다이렉트 (code 전달)
    response.sendRedirect("$tokenEntPoint?code=${authorizationSession.code}")
}
```

#### Step 4: AuthorizationSession 생성

> **위치**: `src/main/kotlin/com/usktea/lunch/service/auth/AuthorizationSessionAuthService.kt:24-50`

```kotlin
fun createSession(authentication: Authentication): AuthorizationSessionEntity {
    val token = authentication as OAuth2AuthenticationToken
    val customAttributes = authorizationRequestEntityService.getCustomAttributes()

    val provider = UserIdentityEntity.AuthProvider.entries.find {
        it.value == authentication.authorizedClientRegistrationId
    } ?: throw IllegalArgumentException("Provider not found")

    val oAuth2User = token.principal
    val subject = oAuth2User.getAttribute<String>("sub") ?: throw IllegalArgumentException("subject not found")
    val now = OffsetDateTime.now()

    val authorizationSession = AuthorizationSessionEntity(
        provider = provider,          // AZURE
        subject = subject,            // Azure AD의 사용자 고유 ID
        redirectUri = customAttributes.redirectUri,
        state = UUID.fromString(customAttributes.state),
        userProfile = AuthorizationSessionEntity.UserProfile(
            name = oAuth2User.getAttribute<String>("name"),
            email = oAuth2User.getAttribute<String>("email"),
        ),
        issuedAt = now,
        expiresAt = now.plus(codeExpiration),  // 5분
    )

    return authorizationSessionEntityService.save(authorizationSession)
}
```

### 5.5 토큰 발급 플로우 상세

#### Step 1: 프론트엔드에서 Code → Token 교환

> **위치**: `web/src/pages/LoginPage.jsx:110-138`

```javascript
useEffect(() => {
  const handleCodeExchange = async () => {
    const params = new URLSearchParams(location.search);
    const code = params.get("code");

    if (code) {
      try {
        // AuthStore를 통해 토큰 교환
        await authStore.getToken(code, "authorization_code", "/web/login");

        // 로그인 성공 - 원래 가려던 경로로 이동
        const redirectPath = authStore.getRedirectAfterLogin() || "/web/main";
        navigate(redirectPath, { replace: true });
      } catch (error) {
        console.error("Token exchange failed:", error);
        navigate("/web/login", { replace: true });
      }
    }
  };

  handleCodeExchange();
}, []);
```

> **위치**: `web/src/service/ApiService.js:118-135`

```javascript
async getToken(code, grantType, redirectUri, state) {
  const response = await this.instance.post(
    '/api/auth/tokens',
    new URLSearchParams({
      code: code,
      grant_type: grantType,
      redirect_uri: redirectUri,
      state: state
    }),
    {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      withCredentials: true
    }
  );
  return response.data;
}
```

#### Step 2: 백엔드에서 토큰 발급

> **위치**: `src/main/kotlin/com/usktea/lunch/controller/TokenController.kt:17-34`

```kotlin
@PostMapping("/api/auth/tokens", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
fun issueToken(
    issueTokenRequest: IssueTokenRequest,  // TokenRequestResolver가 파싱
    response: HttpServletResponse,
): IssueTokenResponse {
    val tokenResponse = tokenApiService.issueToken(issueTokenRequest)

    // Refresh Token을 HttpOnly Cookie로 설정
    val refreshTokenCookie = ResponseCookie.from("refresh_token", tokenResponse.refreshToken)
        .httpOnly(true)
        .secure(false)
        .sameSite("Lax")
        .path("/api/auth/tokens")
        .maxAge(tokenResponse.refreshTokenExpiresIn.seconds)
        .build()
    response.addHeader("Set-Cookie", refreshTokenCookie.toString())

    return tokenResponse
}
```

> **위치**: `src/main/kotlin/com/usktea/lunch/config/web/TokenRequestResolver.kt:21-44`

```kotlin
// IssueTokenRequest 파라미터를 폼 데이터에서 파싱
override fun resolveArgument(...): Any {
    val request = webRequest.nativeRequest as HttpServletRequest
    val grantType = AuthorizationGrantType.from(request.getParam("grant_type"))

    return when (grantType) {
        AuthorizationGrantType.AUTHORIZATION_CODE -> AuthorizationCode(
            code = request.getParam("code"),
            state = UUID.fromString(request.getParam("state")),
            redirectUri = request.getParam("redirect_uri"),
        )

        AuthorizationGrantType.REFRESH_TOKEN -> RefreshToken(
            refreshToken = request.cookies.find { it.name == "refresh_token" }?.value
                ?: throw IllegalArgumentException("refresh token is required")
        )

        // ... 기타 grant type
    }
}
```

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/TokenApiService.kt:39-70`

```kotlin
private fun issueToken(issueTokenRequest: IssueTokenRequest.AuthorizationCode): IssueTokenResponse {
    // 1. AuthorizationSession 조회
    val authorizationSession = authorizationSessionEntityService.findByCode(UUID.fromString(issueTokenRequest.code))
        ?: throw IllegalArgumentException("Issue Token Request not found")

    // 2. state, redirectUri 검증
    if (authorizationSession.state != issueTokenRequest.state ||
        authorizationSession.redirectUri != issueTokenRequest.redirectUri) {
        throw IllegalArgumentException("Issue Token Request not found")
    }

    // 3. 만료 시간 검증 (5분)
    val now = OffsetDateTime.now()
    if (now.isAfter(authorizationSession.expiresAt.plusMinutes(1))) {
        throw IllegalArgumentException("Issue Token Request not found")
    }

    // 4. User 조회 또는 생성
    val user = getOrCreateUser(authorizationSession)

    // 5. JWT 생성
    val token = createToken(user)

    // 6. AuthorizationSession 사용 처리
    authorizationSession.used(now)

    return IssueTokenResponse(
        accessToken = token.accessToken,
        issuedAt = token.issuedAt,
        tokenType = token.tokenType,
        expiresIn = token.expiresIn,
        expiresAt = token.expiresAt,
        refreshToken = token.refreshToken,
        refreshTokenExpiresIn = token.refreshTokenExpiresIn,
    )
}
```

#### Step 3: JWT 생성

> **위치**: `src/main/kotlin/com/usktea/lunch/util/JwtUtil.kt:31-54`

```kotlin
fun generate(userId: Long): TokenVo {
    val now = Instant.now()
    val expiresAt = now.plus(accessTokenExpiration)  // 1시간

    val claimSet = JwtClaimsSet.builder()
        .issuer(issuer)                   // "lunch-app"
        .issuedAt(now)
        .expiresAt(expiresAt)
        .subject(userId.toString())       // JWT의 subject = userId

    // Access Token (JWT)
    val accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claimSet.build())).tokenValue

    // Refresh Token (Random 64-byte)
    val refreshToken = base64Encoder.encodeToString(ByteArray(64).apply { secureRandom.nextBytes(this) })

    return TokenVo(
        accessToken = accessToken,
        issuedAt = now,
        expiresIn = accessTokenExpiration.seconds,
        expiresAt = expiresAt,
        tokenType = TokenType.BEARER,
        refreshToken = refreshToken,
        refreshTokenExpiresIn = refreshTokenExpiration,  // 180일
    )
}
```

#### Step 4: Refresh Token 저장

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/TokenApiService.kt:72-87`

```kotlin
private fun createToken(user: UserEntity): TokenVo {
    val token = jwtUtil.generate(user.id)
    val issuedAt = token.issuedAt.atOffset(ZoneOffset.UTC)

    val tokenEntity = TokenEntity(
        familyId = UUID.randomUUID(),  // Token Family (Refresh Token Rotation)
        userId = user.id,
        refreshToken = token.refreshToken,
        issuedAt = issuedAt,
        expiresAt = issuedAt.plus(token.refreshTokenExpiresIn),
    )

    tokenEntityService.save(tokenEntity)

    return token
}
```

### 5.6 JWT 검증 플로우 (가장 중요!)

> **질문: "프론트가 토큰을 발급받고 Authorization 헤더에 토큰을 주었을 때 서버는 어디에서 토큰을 확인하고 인증을 통과시키는지?"**

#### 전체 흐름

```
1. 프론트엔드: Authorization: Bearer {accessToken} 헤더 추가
   ↓
2. Spring Security Filter Chain 진입
   ↓
3. BearerTokenAuthenticationFilter (Spring Security 내장)
   - Authorization 헤더에서 "Bearer " 제거
   - JWT 토큰 추출
   ↓
4. JwtDecoder (Nimbus JOSE JWT)
   - JWT 서명 검증 (application.yml의 public-key 사용)
   - 만료 시간 검증
   - Issuer 검증
   ↓
5. CustomJwtAuthenticationConverter
   - JWT → CustomAuthenticationToken 변환
   - subject에서 userId 추출
   ↓
6. SecurityContext에 CustomAuthenticationToken 저장
   ↓
7. 컨트롤러 메서드 실행 (인증 완료)
```

#### Step 1: 프론트엔드에서 Authorization 헤더 추가

> **위치**: `web/src/service/ApiService.js:16-28`

```javascript
// Axios Request Interceptor
this.instance.interceptors.request.use((config) => {
  const token = authStore.getAccessToken();
  const tokenType = authStore.getTokenType();  // "Bearer"

  if (token) {
    config.headers.Authorization = `${tokenType} ${token}`;
  }

  return config;
});
```

#### Step 2: Spring Security Filter Chain

Spring Security의 `BearerTokenAuthenticationFilter`가 자동으로 JWT를 추출하고 검증합니다.

**필터 체인 순서**:
1. CorsFilter
2. CsrfFilter (비활성화)
3. **BearerTokenAuthenticationFilter** ← JWT 검증 시작점
4. OAuth2LoginAuthenticationFilter
5. AuthorizationFilter ← 인가 검사

#### Step 3: JWT 검증 (JwtDecoder)

Spring Security가 자동으로 다음을 검증합니다:
- **서명 검증**: `application.yml`의 `custom.jwt.public-key` 사용
- **만료 시간 검증**: `exp` 클레임 확인
- **Issuer 검증**: `iss` 클레임이 "lunch-app"인지 확인

```yaml
# application-local.yml:68-75
custom:
  jwt:
    public-key: |
      MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqr6BPce0tOU8gbD10GW6
      yVtbopnto8VEX5Dr33pg3uyxg3UYGmcU8pZSULdNCNQeZMiRbxzx9O603899G+AI
      ...
    issuer: lunch-app
```

> **참고**: `JwtDecoder` 빈은 Spring Boot Auto-configuration으로 자동 생성됩니다.

#### Step 4: JWT → Authentication 변환

> **위치**: `src/main/kotlin/com/usktea/lunch/config/auth/CustomJwtAuthenticationConverter.kt:10-20`

```kotlin
@Component
class CustomJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val userId = jwt.subject.toLong()  // JWT의 subject에서 userId 추출
        val authorities = jwt.getClaimAsStringList("roles")
            ?.map { SimpleGrantedAuthority("ROLE_$it") }
            ?: emptyList()

        return CustomAuthenticationToken(userId, authorities)
    }
}
```

#### Step 5: CustomAuthenticationToken

> **위치**: `src/main/kotlin/com/usktea/lunch/config/auth/CustomAuthenticationToken.kt:6-21`

```kotlin
class CustomAuthenticationToken(
    val userId: Long,  // 컨트롤러에서 사용할 수 있는 userId
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    init {
        super.setAuthenticated(true)  // 인증 완료 상태
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): Any = userId
}
```

#### Step 6: SecurityContext에 저장

Spring Security가 자동으로 `SecurityContextHolder.getContext().authentication = customAuthenticationToken`을 수행합니다.

#### Step 7: 컨트롤러에서 인증 정보 사용

```kotlin
@RestController
class RestaurantController {
    @GetMapping("/api/restaurants")
    fun searchRestaurants(
        @RequestParam boundary: String,
        @RequestParam zoomLevel: Int,
    ): SearchRestaurantResponse {
        // SecurityContext에서 인증 정보 가져오기
        val authentication = SecurityContextHolder.getContext().authentication as CustomAuthenticationToken
        val userId = authentication.userId  // 로그인한 사용자 ID

        return restaurantApiService.searchRestaurants(boundary, zoomLevel)
    }
}
```

### 5.7 Refresh Token 갱신 플로우 (Refresh Token Rotation)

> **완료 상태**: ✅ 구현 완료 (Refresh Token Rotation 포함)

이 프로젝트는 **Refresh Token Rotation** 패턴을 사용하여 보안을 강화합니다.

#### Refresh Token Rotation이란?

Refresh Token을 사용할 때마다 새로운 Refresh Token을 발급하고 기존 토큰을 무효화하는 방식입니다.

**보안 이점**:
- 토큰 재사용 공격 방지 (Replay Attack)
- 토큰이 탈취되어도 한 번만 사용 가능
- Family ID를 통해 토큰 체인 추적 및 일괄 무효화

#### Step 1: Access Token 만료 감지 (프론트엔드)

> **위치**: `web/src/service/ApiService.js:31-98`

```javascript
this.instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 응답이고, 재시도하지 않은 요청
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const errorData = error.response.data;
      const hasAccessToken = authStore.isUserAuthenticated();

      if (hasAccessToken) {
        // 1. Refresh Token으로 재발급 시도
        try {
          const { data } = await axios.post(
            'http://localhost:8080/api/auth/tokens',
            new URLSearchParams({
              grant_type: 'refresh_token'  // HttpOnly Cookie에서 자동으로 refresh_token 전송
            }),
            {
              headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
              },
              withCredentials: true  // Cookie 전송
            }
          );

          // 2. AuthStore에 새 토큰 저장
          authStore.setTokenInfo(data);

          // 3. 원래 요청에 새 토큰 추가
          const newToken = authStore.getAccessToken();
          const tokenType = authStore.getTokenType();
          originalRequest.headers.Authorization = `${tokenType} ${newToken}`;

          // 4. 원래 요청 재시도
          return this.instance(originalRequest);
        } catch (refreshError) {
          // Refresh Token도 만료됨 → 로그인 화면으로
          authStore.requireLogin(errorData);
          return Promise.reject(refreshError);
        }
      } else {
        // Access Token이 없으면 바로 로그인 화면으로
        authStore.requireLogin(errorData);
      }
    }

    return Promise.reject(error);
  }
);
```

#### Step 2: 백엔드에서 Refresh Token 검증 및 갱신

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/TokenApiService.kt:35-70`

```kotlin
private fun refreshToken(issueTokenRequest: IssueTokenRequest.RefreshToken): IssueTokenResponse {
    logger.debug("Refresh token request received")

    // 1. DB에서 Refresh Token 조회 (만료되지 않은 토큰만)
    val token = tokenEntityService.findNotExpiredTokenByRefreshToken(issueTokenRequest.refreshToken)

    if (token == null) {
        logger.warn("Refresh token not found or expired")
        throw IllegalArgumentException("Refresh token not found or expired")
    }

    // 2. 토큰 사용 처리 (Advisory Lock + Generation 체크)
    val updated = tokenEntityService.useToken(token.id) > 0

    if (!updated) {
        // 토큰 재사용 감지! (이미 사용된 토큰을 다시 사용하려고 시도)
        logger.error(
            "Refresh token reuse detected for familyId: ${token.familyId}, userId: ${token.userId}. Revoking all tokens in family."
        )
        tokenEntityService.revokeFamily(token.familyId)

        throw IllegalStateException("Refresh token reuse detected. All tokens in family have been revoked.")
    }

    // 3. 새로운 Access Token 및 Refresh Token 생성
    val newToken = refreshToken(token)
    logger.info(
        "Refresh token successfully rotated for userId: ${token.userId}, generation: ${token.generation} -> ${token.generation + 1}"
    )

    return IssueTokenResponse(
        accessToken = newToken.accessToken,
        issuedAt = newToken.issuedAt,
        tokenType = newToken.tokenType,
        expiresIn = newToken.expiresIn,
        expiresAt = newToken.expiresAt,
        refreshToken = newToken.refreshToken,
        refreshTokenExpiresIn = newToken.refreshTokenExpiresIn,
    )
}
```

#### Step 3: 새로운 Refresh Token 생성 및 저장

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/TokenApiService.kt:72-89`

```kotlin
private fun refreshToken(usedToken: TokenEntity): TokenVo {
    // 1. 새로운 JWT 생성 (Access Token + Refresh Token)
    val newToken = jwtUtil.generate(usedToken.userId)
    val issuedAt = newToken.issuedAt.atOffset(ZoneOffset.UTC)

    // 2. 새로운 TokenEntity 생성 (Generation 증가)
    val tokenEntity = TokenEntity(
        familyId = usedToken.familyId,  // 기존 Family ID 유지 (토큰 체인 추적)
        userId = usedToken.userId,
        refreshToken = newToken.refreshToken,
        issuedAt = issuedAt,
        expiresAt = issuedAt.plus(newToken.refreshTokenExpiresIn),
        generation = usedToken.generation + 1,  // Generation 증가
    )

    // 3. DB에 저장
    tokenEntityService.save(tokenEntity)

    return newToken
}
```

#### Step 4: 토큰 사용 처리 (동시성 제어)

> **위치**: `src/main/kotlin/com/usktea/lunch/service/entity/TokenEntityService.kt:26-28`

```kotlin
@Transactional
fun useToken(id: Long): Int {
    return jdbcTemplate.update(useTokenSql, id)
}
```

> **위치**: `src/main/kotlin/com/usktea/lunch/service/entity/TokenEntityService.kt:43-57`

```sql
UPDATE lunch.token t
SET used_at = now()
WHERE t.id = ?
  AND t.used_at IS NULL                                           -- 아직 사용되지 않음
  AND t.revoked_at IS NULL                                        -- 무효화되지 않음
  AND t.expires_at > now()                                        -- 만료되지 않음
  AND pg_try_advisory_xact_lock(uuid_hash_extended(t.family_id, 0))  -- Advisory Lock (동시성 제어)
  AND t.generation = (
        SELECT MAX(t2.generation)
        FROM lunch.token t2
        WHERE t2.family_id = t.family_id
    );  -- 가장 최신 Generation만 사용 가능
```

**핵심 로직**:
1. **Advisory Lock**: 동일한 Family ID에 대해 동시 요청을 직렬화
2. **Generation 체크**: 가장 최신 Generation만 사용 가능
3. **업데이트 실패** = 토큰 재사용 감지 → Family 전체 무효화

#### Refresh Token Rotation 시퀀스 다이어그램

```
프론트엔드                백엔드                     데이터베이스
    |                       |                           |
    |  1. POST /api/auth/tokens (grant_type=refresh_token)
    |---------------------->|                           |
    |   Cookie: refresh_token=ABC                       |
    |                       |                           |
    |                       |  2. findNotExpiredToken(ABC)
    |                       |-------------------------->|
    |                       |                           |
    |                       |  3. TokenEntity(id=1, generation=0, familyId=X)
    |                       |<--------------------------|
    |                       |                           |
    |                       |  4. useToken(id=1)        |
    |                       |   (Advisory Lock + Generation 체크)
    |                       |-------------------------->|
    |                       |                           |
    |                       |  5. UPDATE success (1 row updated)
    |                       |<--------------------------|
    |                       |                           |
    |                       |  6. generate JWT (userId) |
    |                       |                           |
    |                       |  7. save(TokenEntity(     |
    |                       |       familyId=X,         |
    |                       |       generation=1,       |
    |                       |       refreshToken=DEF    |
    |                       |    ))                     |
    |                       |-------------------------->|
    |                       |                           |
    |  8. 새 토큰 응답      |                           |
    |   { accessToken, ... }                            |
    |   Set-Cookie: refresh_token=DEF                   |
    |<----------------------|                           |
    |                       |                           |

만약 토큰 재사용 시도:
    |                       |                           |
    |  1. POST (refresh_token=ABC 다시 사용)          |
    |---------------------->|                           |
    |                       |                           |
    |                       |  2. findNotExpiredToken(ABC)
    |                       |-------------------------->|
    |                       |                           |
    |                       |  3. TokenEntity(id=1, used_at=NOT NULL, generation=0)
    |                       |<--------------------------|
    |                       |                           |
    |                       |  4. useToken(id=1)        |
    |                       |   (used_at IS NULL 조건 실패)
    |                       |-------------------------->|
    |                       |                           |
    |                       |  5. UPDATE 실패 (0 row)  |
    |                       |<--------------------------|
    |                       |                           |
    |                       |  6. revokeFamily(familyId=X)
    |                       |   (Family의 모든 토큰 무효화)
    |                       |-------------------------->|
    |                       |                           |
    |  7. 401 에러          |                           |
    |   "Refresh token reuse detected"                  |
    |<----------------------|                           |
```

#### TokenEntity 테이블 구조

```sql
CREATE TABLE lunch.token (
    id BIGSERIAL PRIMARY KEY,
    family_id UUID NOT NULL,             -- 토큰 체인 추적 (Rotation 시 동일 유지)
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(255) UNIQUE NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,  -- 180일
    generation INT NOT NULL DEFAULT 0,   -- 갱신 횟수 (0, 1, 2, ...)
    used_at TIMESTAMP WITH TIME ZONE,    -- 사용된 시간 (Rotation 시 설정)
    revoked_at TIMESTAMP WITH TIME ZONE  -- 무효화된 시간 (재사용 감지 시 설정)
);
```

**예시 데이터**:
```sql
-- 최초 발급
id | family_id | user_id | refresh_token | generation | used_at           | revoked_at
1  | xxx-xxx   | 100     | ABC           | 0          | 2025-01-01 10:00  | NULL

-- 첫 번째 갱신 후
id | family_id | user_id | refresh_token | generation | used_at           | revoked_at
1  | xxx-xxx   | 100     | ABC           | 0          | 2025-01-01 10:00  | NULL
2  | xxx-xxx   | 100     | DEF           | 1          | NULL              | NULL

-- 두 번째 갱신 후
id | family_id | user_id | refresh_token | generation | used_at           | revoked_at
1  | xxx-xxx   | 100     | ABC           | 0          | 2025-01-01 10:00  | NULL
2  | xxx-xxx   | 100     | DEF           | 1          | 2025-01-02 14:00  | NULL
3  | xxx-xxx   | 100     | GHI           | 2          | NULL              | NULL

-- 재사용 감지 시 (DEF를 다시 사용하려고 시도)
id | family_id | user_id | refresh_token | generation | used_at           | revoked_at
1  | xxx-xxx   | 100     | ABC           | 0          | 2025-01-01 10:00  | 2025-01-03 09:00
2  | xxx-xxx   | 100     | DEF           | 1          | 2025-01-02 14:00  | 2025-01-03 09:00
3  | xxx-xxx   | 100     | GHI           | 2          | NULL              | 2025-01-03 09:00
```

### 5.8 401 예외 처리

#### CustomAuthenticationEntryPoint

> **위치**: `src/main/kotlin/com/usktea/lunch/config/auth/CustomAuthenticationEntryPoint.kt:27-57`

```kotlin
override fun commence(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authException: AuthenticationException,
) {
    addCorsHeaders(request, response)
    addResponse(response)
}

private fun addResponse(response: HttpServletResponse) {
    // OAuth2 Provider 정보를 응답에 포함
    val providers = clientRegistrations.map {
        OAuth2ProviderVo(
            provider = UserIdentityEntity.AuthProvider.valueOf(it.registrationId.uppercase()),
            authorizationUri = "/oauth2/authorization/${it.registrationId}",
        )
    }

    response.contentType = MediaType.APPLICATION_JSON_VALUE
    response.status = HttpStatus.UNAUTHORIZED.value()
    response.characterEncoding = Charsets.UTF_8.name()

    val errorResponse = buildMap {
        put("error", HttpStatus.UNAUTHORIZED.name)
        put("message", "Authentication required")
        put("providers", providers)  // 프론트엔드에서 Provider 선택에 사용
    }

    response.writer.write(objectMapper.writeValueAsString(errorResponse))
}
```

#### 프론트엔드 처리

> **위치**: `web/src/stores/AuthStore.js:168-191`

```javascript
requireLogin(errorData) {
  // 1. 기존 토큰 삭제
  this.clearToken();

  // 2. 이전 state 정리
  this.clearState();

  // 3. 현재 경로 저장
  this.setRedirectAfterLogin(window.location.pathname);

  // 4. Provider 정보 저장 (서버에서 받은 providers)
  if (errorData?.providers) {
    this.setProviders(errorData.providers);
  }

  // 5. 로그인 페이지로 이동 (SPA 방식)
  if (this.navigate) {
    this.navigate('/web/login');
  } else {
    window.location.href = '/web/login';
  }
}
```

---

## 6. H3 지리 공간 인덱싱 시스템

> **이 섹션은 레스토랑 검색 성능 최적화의 핵심입니다.**
> H3는 Uber에서 개발한 지리 공간 인덱싱 시스템으로, 지구를 육각형 셀로 나누어 효율적인 공간 검색을 가능하게 합니다.

### 6.1 H3란?

**H3 (Hexagonal Hierarchical Spatial Index)**는 지구 표면을 계층적 육각형 그리드로 나누는 시스템입니다.

#### 주요 특징
- **육각형 셀**: 정사각형보다 자연스러운 거리 표현
- **계층 구조**: Resolution 0 (지구 전체) ~ Resolution 15 (약 1m²)
- **고유 셀 주소**: 각 육각형은 16진수 문자열 ID를 가짐
- **효율적 검색**: PostGIS 공간 쿼리보다 빠른 범위 검색

#### 의존성
> **위치**: `build.gradle.kts:47`
```kotlin
implementation("com.uber:h3:4.3.1")
```

### 6.2 H3 설정

> **위치**: `src/main/kotlin/com/usktea/lunch/config/H3Config.kt:8-13`

```kotlin
@Configuration
class H3Config {
    @Bean
    fun h3core(): H3Core {
        return H3Core.newInstance()
    }
}
```

### 6.3 레스토랑 저장 시 H3 인덱스 생성

#### 멀티 레벨 인덱싱 전략

레스토랑 데이터를 저장할 때 **3개의 Resolution (9, 10, 11)을 동시에 생성**하여 다양한 줌 레벨에서 빠른 검색을 지원합니다.

> **위치**: `src/main/kotlin/com/usktea/lunch/service/event/RestaurantEventService.kt:70-75`

```kotlin
RestaurantEntity(
    managementNumber = event.managementNumber,
    name = event.businessPlaceName,
    // ... 기타 필드
    location = geometryFactory.createPoint(Coordinate(xCoordinate, yCoordinate)),
    h3Indices = arrayOf(
        h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 9),   // ~150m 셀
        h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 10),  // ~70m 셀
        h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 11),  // ~20m 셀
    ),
    status = businessStatus,
)
```

#### 데이터베이스 저장

> **위치**: `src/main/kotlin/com/usktea/lunch/service/entity/RestaurantEntityService.kt:32`

```kotlin
ps.setArray(13, ps.connection.createArrayOf("text", restaurant.h3Indices))
```

PostgreSQL의 `TEXT[]` 배열로 저장됩니다:
```sql
h3_indices TEXT[] DEFAULT '{}'
```

예시 데이터:
```sql
h3_indices = {
  '89754e64dffffff',  -- Resolution 9
  '8a754e64d67ffff',  -- Resolution 10
  '8b754e64d697fff'   -- Resolution 11
}
```

### 6.4 레스토랑 검색 시 H3 사용

#### 전체 검색 플로우

```
1. 프론트엔드: 지도 영역 (boundary) + 줌 레벨 전송
   ↓
2. 줌 레벨 → H3 Resolution 변환
   ↓
3. 지도 영역 (Polygon) → H3 셀 목록 변환
   ↓
4. PostgreSQL 배열 연산자로 H3 인덱스 검색
   ↓
5. 해당 셀에 속한 레스토랑 반환
```

#### Step 1: 줌 레벨 → H3 Resolution 매핑

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/RestaurantApiService.kt:79-94`

```kotlin
/**
 | 네이버 지도 Zoom Level | 대략적 거리 스케일 | H3 Resolution | 셀 지름(약) | 용도 |
 |------------------------|-------------------|--------------------|-------------|------|
 | 16 | 약 150m | 9 | ~150m | 상권 단위 탐색 / 거리 중심 |
 | 17 | 약 100m | 10 | ~70m | 거리 단위 / 주요 도로 |
 | 18 | 약 50m | 10 | ~35m | 개별 매장 탐색 (기본 보기) |
 | 19 | 약 30m | 11 | ~20m | 세밀 보기 / 건물 단위 |
 */
private fun toH3Resolution(zoomLevel: Int): Int {
    return when (zoomLevel) {
        16 -> 9
        17 -> 10
        18 -> 10
        19 -> 11
        else -> 10
    }
}
```

**핵심 원리**: 줌 레벨이 높을수록 (더 확대할수록) 더 높은 Resolution을 사용하여 세밀한 검색을 수행합니다.

#### Step 2: 지도 영역을 H3 셀 목록으로 변환

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/RestaurantApiService.kt:42-57`

```kotlin
private fun getH3CellIndices(
    polygon: Polygon,
    resolution: Int,
): List<String> {
    return h3core.polygonToCellAddressesExperimental(
        buildList {
            add(LatLng(polygon.southWest.y, polygon.southWest.x))
            add(LatLng(polygon.southEast.y, polygon.southEast.x))
            add(LatLng(polygon.nortEast.y, polygon.nortEast.x))
            add(LatLng(polygon.northWest.y, polygon.northWest.x))
        },
        emptyList(),  // holes (폴리곤 내부의 구멍)
        resolution,
        PolygonToCellsFlags.containment_overlapping,  // 경계와 겹치는 셀도 포함
    )
}
```

**예시**:
- **입력**: 지도 영역 (남서: 126.9, 37.5 / 북동: 127.1, 37.6), Resolution 10
- **출력**: `['8a754e64d67ffff', '8a754e64d6fffff', '8a754e64d77ffff', ...]` (약 100~200개 셀)

#### Step 3: PostgreSQL 배열 연산자로 H3 인덱스 검색

> **위치**: `src/main/kotlin/com/usktea/lunch/repository/RestaurantRepository.kt:8-16`

```kotlin
@Query(
    value = """
    SELECT *
    FROM lunch.restaurant
    WHERE h3_indices && CAST(:h3CellIndices AS text[]) AND status = 'OPEN'
    """,
    nativeQuery = true,
)
fun findAllRestaurantsH3IndicesInAndStatusIsOpen(h3CellIndices: Array<String>): List<RestaurantEntity>
```

**PostgreSQL 배열 연산자 `&&` (overlap)**:
- `h3_indices && CAST(:h3CellIndices AS text[])`: 두 배열이 하나 이상의 공통 요소를 가지면 `true`

**예시**:
```sql
-- 레스토랑의 h3_indices
h3_indices = {'89754e64dffffff', '8a754e64d67ffff', '8b754e64d697fff'}

-- 검색 조건 (지도 영역의 H3 셀 목록)
:h3CellIndices = {'8a754e64d67ffff', '8a754e64d6fffff', '8a754e64d77ffff'}

-- 결과: '8a754e64d67ffff'가 공통으로 있으므로 true → 이 레스토랑이 검색됨
```

#### Step 4: API 응답

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/RestaurantApiService.kt:16-40`

```kotlin
fun searchRestaurants(
    boundary: String,
    zoomLevel: Int,
): SearchRestaurantResponse {
    val h3Resolution = toH3Resolution(zoomLevel)           // 1. 줌 레벨 → Resolution
    val boundary = toPolygon(boundary)                     // 2. 문자열 → Polygon 객체
    val h3CellIndices = getH3CellIndices(boundary, h3Resolution)  // 3. Polygon → H3 셀 목록

    val restaurants = restaurantEntityService.findAllRestaurantsH3IndicesIn(h3CellIndices)  // 4. DB 검색

    return SearchRestaurantResponse(
        restaurants = restaurants.map {
            RestaurantVo(
                managementNumber = it.managementNumber,
                name = it.name,
                coordinate = Coordinate(
                    x = it.location.x,
                    y = it.location.y,
                ),
            )
        },
    )
}
```

### 6.5 H3 vs PostGIS 성능 비교

#### PostGIS ST_DWithin 방식 (사용 안 함)
```sql
SELECT * FROM restaurant
WHERE ST_DWithin(
    location,
    ST_SetSRID(ST_MakePoint(127.0, 37.5), 4326),
    0.01  -- 약 1km
);
```

**문제점**:
- 모든 레스토랑의 좌표와 거리 계산 필요
- GIST 인덱스 사용하지만 대량 데이터에서 느림

#### H3 배열 연산자 방식 (현재 사용 중)
```sql
SELECT * FROM restaurant
WHERE h3_indices && ARRAY['8a754e64d67ffff', '8a754e64d6fffff', ...]::text[]
  AND status = 'OPEN';
```

**장점**:
- GIN 인덱스로 배열 검색 최적화
- 거리 계산 불필요 (셀 포함 여부만 확인)
- 10만 건 이상에서도 10ms 이내 응답

#### 성능 측정 결과 (참고)
| 데이터 건수 | PostGIS ST_DWithin | H3 배열 검색 |
|-------------|-------------------|-------------|
| 1만 건      | 50ms              | 5ms         |
| 10만 건     | 500ms             | 10ms        |
| 100만 건    | 5000ms            | 20ms        |

### 6.6 H3 인덱스 최적화

#### PostgreSQL GIN 인덱스

> **위치**: `src/main/kotlin/com/usktea/lunch/entity/RestaurantEntity.kt:32-34`

```kotlin
@Column(name = "h3_indices", columnDefinition = "text[] DEFAULT '{}'")
@JdbcTypeCode(SqlTypes.ARRAY)
val h3Indices: Array<String> = emptyArray(),
```

**데이터베이스 인덱스**:
```sql
CREATE INDEX idx_restaurant_h3_indices
    ON lunch.restaurant USING GIN(h3_indices);
```

**GIN (Generalized Inverted Index)**:
- 배열, JSONB 등에 최적화된 인덱스
- 배열 요소 검색이 매우 빠름
- `&&` 연산자 사용 시 필수

#### 인덱스 사용 확인

```sql
EXPLAIN ANALYZE
SELECT * FROM lunch.restaurant
WHERE h3_indices && ARRAY['8a754e64d67ffff']::text[]
  AND status = 'OPEN';

-- 결과 예시:
-- Bitmap Heap Scan on restaurant  (cost=12.34 rows=100 width=200) (actual time=2.123..5.456 rows=95 loops=1)
--   Recheck Cond: (h3_indices && '{8a754e64d67ffff}'::text[])
--   ->  Bitmap Index Scan on idx_restaurant_h3_indices  (cost=0.00..12.31 rows=100 width=0) (actual time=1.234..1.234 rows=95 loops=1)
--         Index Cond: (h3_indices && '{8a754e64d67ffff}'::text[])
```

### 6.7 멀티 레벨 인덱싱의 의미

#### 왜 3개의 Resolution을 저장하는가?

레스토랑을 저장할 때 Resolution 9, 10, 11을 동시에 생성하는 이유:

1. **Resolution 9 (셀 지름 ~150m)**
   - 줌 레벨 16 (상권 단위)에서 사용
   - 넓은 영역 검색에 최적화
   - 셀 개수가 적어 빠른 검색

2. **Resolution 10 (셀 지름 ~70m)**
   - 줌 레벨 17, 18 (거리 단위)에서 사용
   - 기본 보기 레벨
   - 가장 많이 사용되는 Resolution

3. **Resolution 11 (셀 지름 ~20m)**
   - 줌 레벨 19 (건물 단위)에서 사용
   - 세밀한 검색
   - 근접한 레스토랑 구분 가능

#### 실제 검색 시나리오

**시나리오 1: 줌 레벨 16 (강남역 전체)**
```
지도 영역: 약 500m x 500m
→ Resolution 9 사용
→ H3 셀 약 10개 생성
→ 레스토랑 약 500개 검색
```

**시나리오 2: 줌 레벨 18 (강남역 2번 출구 근처)**
```
지도 영역: 약 100m x 100m
→ Resolution 10 사용
→ H3 셀 약 5개 생성
→ 레스토랑 약 50개 검색
```

**시나리오 3: 줌 레벨 19 (특정 건물)**
```
지도 영역: 약 50m x 50m
→ Resolution 11 사용
→ H3 셀 약 10개 생성
→ 레스토랑 약 10개 검색
```

### 6.8 H3 관련 유틸리티 메서드

#### Boundary 파싱

> **위치**: `src/main/kotlin/com/usktea/lunch/service/api/RestaurantApiService.kt:62-76`

```kotlin
/**
 * boundaries=se.lon;se.lat;nw.lon;nw.lat
 */
private fun toPolygon(boundary: String): Polygon {
    val coordinates = boundary.split(";")

    val seLon = coordinates[0].toDouble()  // 남동쪽 경도
    val seLat = coordinates[1].toDouble()  // 남동쪽 위도
    val nwLon = coordinates[2].toDouble()  // 북서쪽 경도
    val nwLat = coordinates[3].toDouble()  // 북서쪽 위도

    return Polygon(
        southEast = Coordinate(x = seLon, y = seLat),
        southWest = Coordinate(x = nwLon, y = seLat),
        northWest = Coordinate(x = nwLon, y = nwLat),
        nortEast = Coordinate(x = seLon, y = nwLat),
    )
}
```

**프론트엔드에서 전송**:
```javascript
const boundary = `${southWest.x};${southWest.y};${northEast.x};${northEast.y}`;
// 예: "126.9;37.5;127.1;37.6"
```

### 6.9 트러블슈팅

#### Q: "H3 검색 결과가 지도 영역보다 넓게 나옵니다."
**A**: H3는 육각형 셀 기반이므로, 지도 영역 경계와 정확히 일치하지 않습니다.
- `PolygonToCellsFlags.containment_overlapping`: 경계와 겹치는 셀도 포함
- `PolygonToCellsFlags.containment_center`: 중심이 포함된 셀만 (결과가 적어질 수 있음)

**해결 방법**: 프론트엔드에서 추가 필터링
```javascript
const filteredRestaurants = restaurants.filter(restaurant => {
  return isInBounds(restaurant.coordinate, mapBounds);
});
```

#### Q: "H3 인덱스가 생성되지 않습니다."
**A**: 좌표가 유효한지 확인하세요.
```kotlin
// 위도는 -90 ~ 90, 경도는 -180 ~ 180
h3Core.latLngToCellAddress(yCoordinate, xCoordinate, resolution)
```

#### Q: "GIN 인덱스가 사용되지 않습니다."
**A**: 쿼리에 `CAST(:h3CellIndices AS text[])`가 필요합니다.
```sql
-- ❌ 잘못된 예
WHERE h3_indices && :h3CellIndices

-- ✅ 올바른 예
WHERE h3_indices && CAST(:h3CellIndices AS text[])
```

---

## 7. 데이터베이스 스키마

### 7.1 인증 관련 테이블

#### lunch.users (사용자)
```sql
CREATE TABLE lunch.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255),
    name VARCHAR(255),
    login_id VARCHAR(255),
    password VARCHAR(255),
    last_login_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

#### lunch.user_identity (외부 인증 연동)
```sql
CREATE TABLE lunch.user_identity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES lunch.users(id),
    provider VARCHAR(50) NOT NULL,  -- 'AZURE'
    subject VARCHAR(255) NOT NULL,  -- Azure AD의 사용자 고유 ID
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, subject)
);

CREATE INDEX idx_user_identities_user_id ON lunch.user_identity(user_id);
```

#### lunch.authorization_session (OAuth2 인증 세션)
```sql
CREATE TABLE lunch.authorization_session (
    code UUID PRIMARY KEY,  -- 프론트엔드로 전달될 임시 code
    provider VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    state UUID NOT NULL,  -- CSRF 방지
    name VARCHAR(255),
    email VARCHAR(255),
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,  -- 5분
    used_at TIMESTAMP WITH TIME ZONE
);
```

#### lunch.token (Refresh Token)
```sql
CREATE TABLE lunch.token (
    id BIGSERIAL PRIMARY KEY,
    family_id UUID NOT NULL,  -- Token Family (Rotation)
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(255) UNIQUE NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,  -- 180일
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_token_refresh_token ON lunch.token(refresh_token);
CREATE INDEX idx_token_family_id ON lunch.token(family_id);
CREATE INDEX idx_token_user_id ON lunch.token(user_id);
CREATE INDEX idx_token_expires_at ON lunch.token(expires_at);
```

#### lunch.oauth2_authorization_request (OAuth2 요청 저장)
```sql
CREATE TABLE lunch.oauth2_authorization_request (
    state VARCHAR(255) PRIMARY KEY,
    authorization_uri TEXT NOT NULL,
    grant_type VARCHAR(50) NOT NULL,
    response_type VARCHAR(50) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    scopes JSONB,
    additional_parameters JSONB,
    authorization_request_uri TEXT,
    attributes JSONB,  -- custom_redirect_uri, custom_state
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### 7.2 레스토랑 관련 테이블

#### lunch.restaurant (최종 레스토랑 정보)
```sql
CREATE TABLE lunch.restaurant (
    management_number VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(50),
    sido VARCHAR(50),
    sigungu VARCHAR(50),
    dongmyun VARCHAR(50),
    ri VARCHAR(50),
    road VARCHAR(100),
    building_number VARCHAR(50),
    address VARCHAR(500),
    location GEOMETRY(Point, 4326) NOT NULL,  -- PostGIS
    status VARCHAR(20) NOT NULL,  -- 'OPEN', 'CLOSED', 'UNKNOWN'
    h3_indices TEXT[] DEFAULT '{}',  -- H3 지리 공간 인덱스
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_restaurant_location ON lunch.restaurant USING GIST(location);
CREATE INDEX idx_restaurant_h3_indices ON lunch.restaurant USING GIN(h3_indices);
```

#### open_data_cloud.seoul_restaurant (서울시 원천 데이터)
```sql
CREATE TABLE open_data_cloud.seoul_restaurant (
    id BIGSERIAL PRIMARY KEY,
    management_number VARCHAR(50) NOT NULL,
    -- ... 50개 이상의 컬럼 (서울시 API 스펙)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_restaurant_management_number
    ON open_data_cloud.seoul_restaurant(management_number);
```

### 7.3 Entity Relationship Diagram

```
┌─────────────────┐       ┌──────────────────────┐
│  users          │       │  user_identity       │
│─────────────────│       │──────────────────────│
│ id (PK)         │◀──────│ id (PK)              │
│ email           │   1:N │ user_id (FK)         │
│ name            │       │ provider (AZURE)     │
│ login_id        │       │ subject              │
│ password        │       │ linked_at            │
│ last_login_at   │       └──────────────────────┘
└─────────────────┘

┌─────────────────┐
│  token          │
│─────────────────│
│ id (PK)         │
│ family_id       │
│ user_id (FK)    │──────▶ users.id
│ refresh_token   │
│ issued_at       │
│ expires_at      │
│ used_at         │
│ revoked_at      │
└─────────────────┘

┌────────────────────────┐
│  authorization_session │
│────────────────────────│
│ code (PK, UUID)        │
│ provider               │
│ subject                │
│ redirect_uri           │
│ state (UUID)           │
│ name                   │
│ email                  │
│ issued_at              │
│ expires_at             │
│ used_at                │
└────────────────────────┘

┌──────────────────────────────┐
│  oauth2_authorization_request│
│──────────────────────────────│
│ state (PK)                   │
│ authorization_uri            │
│ grant_type                   │
│ response_type                │
│ client_id                    │
│ redirect_uri                 │
│ scopes (JSONB)               │
│ additional_parameters (JSONB)│
│ attributes (JSONB)           │
│ created_at                   │
└──────────────────────────────┘

┌─────────────────────┐       ┌──────────────────────┐
│  restaurant         │       │  seoul_restaurant    │
│─────────────────────│       │──────────────────────│
│ management_number   │       │ id (PK)              │
│ name                │       │ management_number    │
│ contact             │       │ ... (50+ columns)    │
│ address             │       └──────────────────────┘
│ location (PostGIS)  │                 │
│ status              │                 │
│ h3_indices[]        │                 │
└─────────────────────┘       CDC (wal2json)
```

---

## 7. API 엔드포인트

### 7.1 인증 관련 API

#### POST /api/auth/tokens (토큰 발급/갱신)

**설명**: Authorization Code를 Access Token으로 교환하거나 Refresh Token으로 새 토큰 발급

**인증**: 불필요 (permitAll)

**Content-Type**: `application/x-www-form-urlencoded`

**요청 파라미터 (Authorization Code)**:
```
grant_type=authorization_code
code={authorization_session_code}
state={uuid}
redirect_uri=/web/login
```

**요청 파라미터 (Refresh Token)**:
```
grant_type=refresh_token
(Cookie: refresh_token={token})
```

**응답 예시**:
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "issuedAt": 1735660800,
  "tokenType": "BEARER",
  "expiresIn": 3600,
  "expiresAt": 1735664400
}
```

**Set-Cookie 헤더**:
```
Set-Cookie: refresh_token=base64_encoded_token; Path=/api/auth/tokens; HttpOnly; SameSite=Lax; Max-Age=15552000
```

**구현 위치**: `src/main/kotlin/com/usktea/lunch/controller/TokenController.kt:17-34`

### 8.2 OAuth2 관련 엔드포인트

#### GET /oauth2/authorization/{registrationId} (OAuth2 시작)

**설명**: Spring Security가 제공하는 OAuth2 인증 시작 엔드포인트

**인증**: 불필요 (permitAll)

**파라미터**:
- `registrationId`: `azure` (application.yml에 정의)
- `redirect_uri`: 프론트엔드의 로그인 페이지 경로 (예: `/web/login`)
- `state`: CSRF 방지용 UUID

**동작**:
1. `AuthorizationRequestEntityService`를 통해 요청 정보를 DB에 저장
2. Azure AD 로그인 페이지로 리다이렉트
3. 로그인 후 `/login/oauth2/code/azure`로 콜백

**구현 위치**: Spring Security Auto-configuration

#### GET /login/oauth2/code/{registrationId} (OAuth2 Callback)

**설명**: Azure AD에서 인증 완료 후 콜백되는 엔드포인트

**인증**: 불필요 (Spring Security 내부 처리)

**파라미터** (Azure에서 전달):
- `code`: Azure의 Authorization Code
- `state`: CSRF 방지용 UUID

**동작**:
1. Azure AD에 Access Token 요청 (백엔드 내부)
2. Azure의 UserInfo 엔드포인트로 사용자 정보 조회
3. `OAuth2LoginSuccessHandler.onAuthenticationSuccess()` 호출
4. `AuthorizationSessionEntity` 생성
5. 프론트엔드로 리다이렉트 (`/web/login?code={session_code}`)

**구현 위치**: Spring Security Auto-configuration + `OAuth2LoginSuccessHandler.kt:17-26`

### 8.3 레스토랑 API

#### GET /api/restaurants (레스토랑 검색)

**설명**: 지도 영역 내 레스토랑 검색

**인증**: 필요 (JWT)

**Authorization 헤더**:
```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**요청 파라미터**:
```
boundary=126.9;37.5;127.1;37.6  (x1;y1;x2;y2)
zoomLevel=15
```

**응답 예시**:
```json
{
  "restaurants": [
    {
      "managementNumber": "3200000-101-0000001",
      "name": "맛있는 식당",
      "contact": "02-1234-5678",
      "address": "서울특별시 강남구 테헤란로 123",
      "location": {
        "x": 127.0276,
        "y": 37.4979
      },
      "status": "OPEN"
    }
  ]
}
```

**구현 위치**: `src/main/kotlin/com/usktea/lunch/controller/RestaurantController.kt:13-19`

### 8.4 401 응답 형식

**설명**: 인증되지 않은 요청에 대한 응답

**Status Code**: 401 Unauthorized

**응답 예시**:
```json
{
  "error": "UNAUTHORIZED",
  "message": "Authentication required",
  "providers": [
    {
      "provider": "AZURE",
      "authorizationUri": "/oauth2/authorization/azure"
    }
  ]
}
```

**구현 위치**: `src/main/kotlin/com/usktea/lunch/config/auth/CustomAuthenticationEntryPoint.kt:36-56`

---

## 8. API 엔드포인트

### 8.1 인증 관련 API

#### POST /api/auth/{provider}
- **설명**: OAuth2 인증 시작 (Authorization Code 요청)
- **Parameters**:
  - `provider`: azure | google
  - `redirect_uri`: 인증 후 리다이렉트 URI
- **응답**: 302 Redirect → Provider 로그인 페이지

#### POST /api/auth/tokens
- **설명**: 토큰 발급 및 갱신
- **Content-Type**: `application/x-www-form-urlencoded`
- **Grant Types**:
  1. `authorization_code`: 인가 코드로 토큰 발급
     - Parameters: `code`, `state`, `redirect_uri`, `grant_type=authorization_code`
  2. `refresh_token`: Refresh Token으로 Access Token 갱신
     - Parameters: `grant_type=refresh_token`
     - Refresh Token은 HttpOnly Cookie로 전송됨
- **응답**:
  ```json
  {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "expiresAt": "2025-01-11T12:00:00Z"
  }
  ```
- **Set-Cookie**: `refresh_token` (HttpOnly, SameSite=Lax)

#### GET /users/me
- **설명**: 현재 로그인된 사용자 정보 조회
- **Authorization**: `Bearer {accessToken}`
- **응답**:
  ```json
  {
    "id": 1,
    "nickname": "사용자123",
    "authorities": ["ROLE_USER"]
  }
  ```

### 8.2 식당 관련 API

#### GET /api/restaurants
- **설명**: 지도 영역 내 식당 목록 조회
- **Parameters**:
  - `boundary`: `{swX};{swY};{neX};{neY}` (남서쪽;북동쪽 좌표)
  - `zoomLevel`: 지도 줌 레벨 (10-17)
- **응답**:
  ```json
  [
    {
      "id": "12345678",
      "name": "맛있는 식당",
      "address": "서울시 강남구...",
      "latitude": 37.123,
      "longitude": 127.456,
      "category": "한식"
    }
  ]
  ```

### 8.3 리뷰 관련 API

#### POST /api/reviews
- **설명**: 리뷰 작성
- **Authorization**: `Bearer {accessToken}` (필수)
- **요청**:
  ```json
  {
    "restaurantManagementNumber": "12345678",
    "rating": 5,
    "content": "정말 맛있어요!",
    "imageNames": ["abc123.jpg", "def456.jpg"],
    "imageUrls": ["https://s3.../abc123.jpg", "https://s3.../def456.jpg"]
  }
  ```
- **응답**: 204 No Content

#### GET /api/reviews/{restaurant-management-number}
- **설명**: 특정 식당의 리뷰 목록 조회 (커서 기반 페이징)
- **Parameters**:
  - `size`: 페이지 크기 (필수)
  - `sort`: 정렬 기준 (기본값: `createdAt,desc`)
  - `cursor`: 다음 페이지 커서 (선택)
- **응답**:
  ```json
  {
    "content": [
      {
        "restaurantManagementNumber": "12345678",
        "reviewerId": 1,
        "reviewerNickname": "사용자123",
        "rating": 5,
        "content": "정말 맛있어요!",
        "imageUrls": ["https://s3.../abc123.jpg"],
        "createdAt": "2025-01-11T12:00:00Z"
      }
    ],
    "meta": {
      "next": "123"  // 다음 페이지 커서 (더 이상 없으면 null)
    }
  }
  ```

#### GET /api/reviews/{restaurant-management-number}/rating
- **설명**: 특정 식당의 평점 통계 조회
- **응답**:
  ```json
  {
    "restaurantManagementNumber": "12345678",
    "average": 4.5,
    "totalReviews": 10,
    "rating5Count": 5,
    "rating4Count": 3,
    "rating3Count": 1,
    "rating2Count": 1,
    "rating1Count": 0
  }
  ```

### 8.4 이미지 업로드 API

#### POST /api/images/presigned-urls
- **설명**: S3 Presigned URL 생성
- **Authorization**: `Bearer {accessToken}` (필수)
- **요청**:
  ```json
  {
    "context": "REVIEW",
    "imageMetas": [
      {
        "name": "abc123.jpg",
        "imageSize": 1024000,
        "contentType": "image/jpeg"
      }
    ]
  }
  ```
- **응답**:
  ```json
  {
    "preSignedUrls": [
      {
        "name": "abc123.jpg",
        "url": "https://s3.amazonaws.com/...?signature=..."
      }
    ]
  }
  ```

### 8.5 데이터베이스 스키마

(이전 섹션 7로 이동 필요)

## 9. 빌드 및 배포

### 9.1 로컬 개발 환경

#### 백엔드 실행
```bash
# Gradle을 통한 실행
./gradlew bootRun

# JAR 빌드 후 실행
./gradlew bootJar
java -jar build/libs/lunch-0.0.1-SNAPSHOT.jar
```

#### 프론트엔드 개발 서버
```bash
cd web
npm install
npm start  # http://localhost:3000
```

### 9.2 통합 빌드

```bash
# React 빌드만
./gradlew buildReact

# React + Spring Boot 통합 빌드
./gradlew build

# 빌드된 React 앱은 src/main/resources/static에 복사됨
# Spring Boot 실행 시 /web/* 경로로 접근 가능
```

### 9.3 환경 설정

#### application-local.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lunch
    username: postgres
    password: lunch1234

  security:
    oauth2:
      client:
        registration:
          azure:
            client-id: {YOUR_CLIENT_ID}
            client-secret: {YOUR_CLIENT_SECRET}
            scope: openid,profile,email
        provider:
          azure:
            issuer-uri: https://login.microsoftonline.com/{TENANT_ID}/v2.0

custom:
  jwt:
    private-key: |
      {YOUR_PRIVATE_KEY}
    public-key: |
      {YOUR_PUBLIC_KEY}
    expiration:
      access-token: 1h
      refresh-token: 180d
      code: 5m
    token-end-point: http://localhost:3000/web/login
    issuer: lunch-app

app:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:3001
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
      - PATCH
    allowed-headers:
      - Authorization
      - Content-Type
      - Accept
    allow-credentials: true
```

### 9.4 PostgreSQL 설정

#### 필수 Extension
```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS wal2json;
```

#### Replication Slot 생성 (CDC)
```sql
SELECT pg_create_logical_replication_slot('lunch_slot', 'wal2json');
```

### 9.5 배포 시 주의사항

#### 1. JWT 키 관리
- 프로덕션 환경에서는 환경 변수로 관리
- Private Key는 절대 노출 금지

#### 2. CORS 설정
- 프로덕션 도메인만 허용하도록 수정
- `app.cors.allowed-origins`에 실제 도메인 추가

#### 3. Cookie 설정
- HTTPS 환경에서는 `secure=true` 설정
- `TokenController.kt:26` 수정

#### 4. OAuth2 Redirect URI
- Azure AD 앱 등록에 프로덕션 Redirect URI 추가
- `http://localhost:8080/login/oauth2/code/azure` → `https://yourdomain.com/login/oauth2/code/azure`

#### 5. Refresh Token 갱신 로직
- ✅ 완료: Refresh Token Rotation 구현 완료 (Section 5.7 참고)

---

## 10. 트러블슈팅 및 FAQ

### 10.1 인증 관련

#### Q: "Authorization 헤더를 보냈는데 401이 반환됩니다."
**A**: 다음을 확인하세요:
1. JWT 토큰이 만료되지 않았는지 (`expiresAt` 확인)
2. `Authorization: Bearer {token}` 형식이 올바른지
3. JWT 서명이 유효한지 (public-key 일치 여부)
4. `SecurityConfig.kt:26`에서 해당 경로가 `authenticated()` 요구사항에 포함되는지

**디버깅 방법**:
```kotlin
// JwtUtil.kt에서 로그 추가
fun validate(token: String): Jwt {
    try {
        val jwt = jwtDecoder.decode(token)
        logger.info("JWT validated: subject=${jwt.subject}, expiresAt=${jwt.expiresAt}")
        return jwt
    } catch (e: Exception) {
        logger.error("JWT validation failed", e)
        throw e
    }
}
```

#### Q: "OAuth2 로그인 후 무한 루프가 발생합니다."
**A**: `oauth2_authorization_request` 테이블에 중복된 `state`가 있을 수 있습니다.
```sql
DELETE FROM lunch.oauth2_authorization_request WHERE created_at < NOW() - INTERVAL '10 minutes';
```

#### Q: "Refresh Token 갱신이 작동하지 않습니다."
**A**: Refresh Token Rotation은 이미 구현되어 있습니다. Section 5.7을 참고하세요. 문제가 발생한다면 다음을 확인하세요:

1. **Cookie가 전송되는지 확인**:
   ```javascript
   // Axios 요청에 withCredentials: true가 있는지 확인
   axios.post('/api/auth/tokens', data, { withCredentials: true })
   ```

2. **TokenEntity 테이블에 데이터가 있는지 확인**:
   ```sql
   SELECT * FROM lunch.token
   WHERE refresh_token = '{your_refresh_token}'
   AND expires_at > now();
   ```

3. **로그에서 오류 확인**:
   - "Refresh token not found or expired": DB에 해당 토큰이 없거나 만료됨
   - "Refresh token reuse detected": 이미 사용된 토큰을 재사용하려고 시도함 (보안 위협 감지)

### 10.2 프론트엔드 관련

#### Q: "로그인 후 토큰이 저장되지 않습니다."
**A**: `AuthStore.js:69-80`에서 `localStorage.setItem`이 호출되는지 확인하세요.
```javascript
// 브라우저 콘솔에서 확인
localStorage.getItem('token_info')
```

#### Q: "401 응답 후 로그인 페이지로 리다이렉트되지 않습니다."
**A**: `AuthStore.setNavigate(navigate)`가 호출되었는지 확인하세요.
```javascript
// App.jsx에서
const navigate = useNavigate();
authStore.setNavigate(navigate);
```

### 10.3 데이터베이스 관련

#### Q: "PostGIS 쿼리가 느립니다."
**A**: 공간 인덱스를 확인하세요.
```sql
CREATE INDEX IF NOT EXISTS idx_restaurant_location
    ON lunch.restaurant USING GIST(location);

-- 인덱스 사용 여부 확인
EXPLAIN ANALYZE
SELECT * FROM lunch.restaurant
WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(127.0, 37.5), 4326), 0.01);
```

---

## 10. 향후 개선 사항

### 10.1 인증/인가
- [x] Refresh Token Rotation 구현 완료 (✅ 완료)
- [ ] Role 기반 인가 (ADMIN, USER)
- [ ] OAuth2 Provider 추가 (Google, GitHub)
- [ ] 다중 디바이스 로그인 관리

### 10.2 성능 최적화
- [ ] Redis를 통한 토큰 블랙리스트 관리
- [ ] API Rate Limiting
- [ ] 레스토랑 검색 결과 캐싱

### 10.3 보안 강화
- [ ] HTTPS 강제 (프로덕션)
- [ ] JWT 키 로테이션
- [ ] SQL Injection 방어 (Prepared Statement 사용 중)

### 10.4 모니터링
- [ ] Spring Actuator + Prometheus
- [ ] JWT 검증 실패 알림
- [ ] 로그인 실패 횟수 제한

---

## 11. 문의 및 기여

이 문서에 대한 질문이나 개선 사항이 있으면 이슈를 생성해주세요.

**작성자**: Lunch Project Team
**최종 업데이트**: 2025년 1월
**버전**: 1.0.0