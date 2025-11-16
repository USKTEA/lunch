# 🍽️ Lunch - 서울시 맛집 추천 시스템

서울시 일반음식점 데이터를 기반으로 한 점심 추천 애플리케이션

## 📌 프로젝트 소개

- **서울 열린데이터광장** API를 통해 일반음식점 데이터 수집
- **PostgreSQL CDC**를 활용한 실시간 데이터 동기화
- **네이버 지도 API**로 정확한 위치 정보 제공
- **Spring Boot + React** 풀스택 애플리케이션

## 🛠️ 기술 스택

### Backend
- **Language:** Kotlin
- **Framework:** Spring Boot 3.3.5
- **Build Tool:** Gradle (Kotlin DSL)
- **JDK:** Java 21
- **Database:** PostgreSQL + PostGIS
- **Authentication:** OAuth 2.0 (Azure AD), JWT

### Frontend
- **Framework:** React 18
- **State Management:** Custom Observer Pattern
- **Styling:** Styled Components
- **Map:** Naver Maps API

### Infrastructure
- **Database:** AWS RDS (PostgreSQL)
- **Storage:** AWS S3 + CloudFront
- **CDC:** PostgreSQL Logical Replication (wal2json)

## 🚀 시작하기

### 사전 요구사항

- JDK 21
- PostgreSQL 16+ (PostGIS 확장 필요)
- Node.js 18+
- Docker (선택사항)

### 환경 변수 설정

#### 1. 백엔드 설정

루트 디렉토리에 `.env` 파일 생성:

```bash
cp .env.example .env
```

`.env` 파일에 실제 값 입력:
- `DATABASE_URL`: PostgreSQL 연결 URL
- `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`: Azure AD OAuth 자격증명
- `SEOUL_OPEN_DATA_APP_KEY`: 서울 열린데이터광장 API 키
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`: 네이버 클라우드 플랫폼 API 키
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`: AWS 자격증명
- `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`: JWT 서명 키

#### 2. 프론트엔드 설정

`web/` 디렉토리에 `.env` 파일 생성:

```bash
cp web/.env.example web/.env
```

`.env` 파일에 실제 값 입력:
- `REACT_APP_NAVER_MAP_CLIENT_ID`: 네이버 지도 API 클라이언트 ID
- `REACT_APP_API_BASE_URL`: 백엔드 API 서버 URL
- `REACT_APP_CLOUDFRONT_DOMAIN`: CloudFront 도메인

### API 키 발급 방법

#### Azure AD OAuth 2.0
1. [Azure Portal](https://portal.azure.com/) 접속
2. Azure Active Directory → App registrations → New registration
3. Redirect URI 설정: `http://localhost:8080/login/oauth2/code/azure`
4. Certificates & secrets에서 Client Secret 생성

#### 서울 열린데이터광장
1. [서울 열린데이터광장](https://data.seoul.go.kr/) 회원가입
2. 마이페이지 → 인증키 신청
3. 일반음식점 API 사용 신청

#### 네이버 클라우드 플랫폼
1. [Naver Cloud Platform](https://www.ncloud.com/) 가입
2. Console → Services → AI·NAVER API → Maps
3. Application 등록 후 Client ID/Secret 발급

#### AWS
1. [AWS IAM](https://console.aws.amazon.com/iam/) 접속
2. Users → Add user
3. Access key 발급 (Programmatic access)
4. S3, CloudFront 권한 부여

### 로컬 실행

#### 백엔드 실행

```bash
# Gradle 빌드 및 실행
./gradlew bootRun

# 또는 JAR 파일 실행
./gradlew bootJar
java -jar build/libs/lunch-0.0.1-SNAPSHOT.jar
```

#### 프론트엔드 실행

```bash
cd web
npm install
npm start
```

#### Docker 실행

```bash
docker-compose up -d
```

## 📁 프로젝트 구조

```
lunch/
├── src/main/kotlin/com/usktea/lunch/
│   ├── client/          # 외부 API 클라이언트 (서울시, 네이버)
│   ├── cdc/             # CDC 이벤트 처리
│   ├── config/          # Spring 설정
│   ├── controller/      # REST API 컨트롤러
│   ├── entity/          # JPA 엔티티
│   ├── listener/        # CDC 리스너
│   ├── repository/      # JPA 리포지토리
│   └── service/         # 비즈니스 로직
├── web/                 # React 프론트엔드
│   ├── src/
│   │   ├── components/  # React 컴포넌트
│   │   ├── stores/      # 상태 관리 Store
│   │   ├── hooks/       # 커스텀 Hooks
│   │   └── service/     # API 서비스
│   └── public/
├── docs/                # 프로젝트 문서
└── docker-compose.yml   # Docker 설정
```

자세한 내용은 [ARCHITECTURE.md](docs/ARCHITECTURE.md) 참고

## 🔐 보안

### ⚠️ 중요

**절대 `.env` 파일을 Git에 커밋하지 마세요!**

민감한 정보는 환경 변수로 관리하며, 프로덕션 환경에서는 다음 사용을 권장합니다:
- AWS Secrets Manager
- Azure Key Vault
- GitHub Secrets (CI/CD)

보안 감사 리포트: [SECURITY_AUDIT_PUBLIC_RELEASE.md](docs/SECURITY_AUDIT_PUBLIC_RELEASE.md)

## 🗂️ 데이터베이스

### PostgreSQL 설정

```sql
-- PostGIS 확장 설치
CREATE EXTENSION IF NOT EXISTS postgis;

-- CDC를 위한 logical replication 설정
-- postgresql.conf
wal_level = logical
max_replication_slots = 10
max_wal_senders = 10

-- Replication Slot 생성
SELECT pg_create_logical_replication_slot('seoul_restaurant', 'wal2json');
```

자세한 마이그레이션 가이드: [database-migration-guide.md](docs/database-migration-guide.md)

## 📚 문서

- [서비스 기획서](docs/서비스_기획_맛집평가시스템.md)
- [아키텍처 가이드](docs/ARCHITECTURE.md)
- [데이터베이스 마이그레이션](docs/database-migration-guide.md)
- [보안 감사 리포트](docs/SECURITY_AUDIT_PUBLIC_RELEASE.md)
- [Public 공개 체크리스트](docs/PUBLIC_RELEASE_CHECKLIST.md)

## 🧪 테스트

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests LunchApplicationTests
```

## 📦 빌드

```bash
# 백엔드 + 프론트엔드 통합 빌드
./gradlew build

# React 빌드만
./gradlew buildReact

# Docker 이미지 빌드
docker build -t lunch:latest .
```

## 🌐 배포

빌드된 React 앱은 `src/main/resources/static`에 포함되어 Spring Boot와 함께 배포됩니다.

- 백엔드 API: `http://localhost:8080/api`
- 프론트엔드: `http://localhost:8080/web`

## 🤝 기여

이 프로젝트는 개인 토이 프로젝트입니다. 이슈나 개선 제안은 환영합니다!

## 📄 라이선스

MIT License

## 👤 작성자

**SUKTAE KIM**
- GitHub: [@USKTEA](https://github.com/USKTEA)

---

**Let's eat lunch! 🍽️**
