# 데이터베이스 마이그레이션 가이드

새로운 PostgreSQL 데이터베이스로 전체 스키마와 설정을 복사하는 가이드입니다.

## 📋 목차
1. [사전 준비사항](#1-사전-준비사항)
2. [PostgreSQL 확장 기능 설치](#2-postgresql-확장-기능-설치)
3. [스키마 생성](#3-스키마-생성)
4. [테이블 생성](#4-테이블-생성)
5. [인덱스 및 제약조건](#5-인덱스-및-제약조건)
6. [CDC (Change Data Capture) 설정](#6-cdc-change-data-capture-설정)
7. [애플리케이션 설정 업데이트](#7-애플리케이션-설정-업데이트)
8. [검증](#8-검증)

---

## 1. 사전 준비사항

### 필요한 정보
- 기존 DB 접속 정보 (호스트, 포트, 사용자명, 비밀번호, 데이터베이스명)
- 새 DB 접속 정보
- PostgreSQL 버전 확인 (최소 9.4 이상 필요)

### 기존 DB에서 스키마 덤프

```bash
# 전체 스키마 추출 (lunch, open_data_cloud)
pg_dump -h <기존_호스트> \
        -U <사용자명> \
        -d <데이터베이스명> \
        --schema-only \
        --no-owner \
        --no-acl \
        --schema=lunch \
        --schema=open_data_cloud \
        -f schema_dump.sql

# 특정 테이블만 추출하려면
pg_dump -h <기존_호스트> \
        -U <사용자명> \
        -d <데이터베이스명> \
        --schema-only \
        --no-owner \
        --no-acl \
        -t lunch.restaurant \
        -t lunch.users \
        -t lunch.user_identity \
        -t lunch.review \
        -t lunch.image \
        -t lunch.token \
        -t lunch.authorization_session \
        -t lunch.oauth2_authorization_request \
        -t open_data_cloud.seoul_restaurant \
        -f schema_tables.sql
```

---

## 2. PostgreSQL 확장 기능 설치

새 데이터베이스에 접속하여 필요한 확장 기능을 설치합니다.

```sql
-- 슈퍼유저 권한으로 실행
\c <새_데이터베이스명>

-- PostGIS 설치 (공간 데이터 타입 지원)
CREATE EXTENSION IF NOT EXISTS postgis;

-- wal2json 플러그인 설치 (CDC용)
-- 주의: 서버에 wal2json이 설치되어 있어야 함
-- Ubuntu/Debian: sudo apt-get install postgresql-<version>-wal2json
-- CentOS/RHEL: sudo yum install wal2json_<version>
-- Docker: wal2json이 포함된 이미지 사용 (예: debezium/postgres)
```

### wal2json 설치 확인

```sql
-- 사용 가능한 출력 플러그인 확인
SELECT * FROM pg_available_extensions WHERE name = 'wal2json';
```

만약 wal2json이 없다면:

**Docker 사용 시:**
```yaml
# docker-compose.yml 예시
services:
  postgres:
    image: debezium/postgres:15-alpine  # wal2json 포함
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    command:
      - "postgres"
      - "-c"
      - "wal_level=logical"
      - "-c"
      - "max_replication_slots=4"
      - "-c"
      - "max_wal_senders=4"
```

**직접 설치 시 (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install postgresql-15-wal2json
sudo systemctl restart postgresql
```

---

## 3. 스키마 생성

```sql
-- 새 DB에 접속
\c <새_데이터베이스명>

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS lunch;
CREATE SCHEMA IF NOT EXISTS open_data_cloud;

-- 권한 설정 (애플리케이션 사용자)
GRANT USAGE ON SCHEMA lunch TO <애플리케이션_사용자>;
GRANT USAGE ON SCHEMA open_data_cloud TO <애플리케이션_사용자>;

GRANT ALL PRIVILEGES ON SCHEMA lunch TO <애플리케이션_사용자>;
GRANT ALL PRIVILEGES ON SCHEMA open_data_cloud TO <애플리케이션_사용자>;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA lunch TO <애플리케이션_사용자>;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA open_data_cloud TO <애플리케이션_사용자>;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA lunch TO <애플리케이션_사용자>;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA open_data_cloud TO <애플리케이션_사용자>;

-- 향후 생성될 객체에 대한 기본 권한
ALTER DEFAULT PRIVILEGES IN SCHEMA lunch GRANT ALL ON TABLES TO <애플리케이션_사용자>;
ALTER DEFAULT PRIVILEGES IN SCHEMA lunch GRANT ALL ON SEQUENCES TO <애플리케이션_사용자>;
ALTER DEFAULT PRIVILEGES IN SCHEMA open_data_cloud GRANT ALL ON TABLES TO <애플리케이션_사용자>;
ALTER DEFAULT PRIVILEGES IN SCHEMA open_data_cloud GRANT ALL ON SEQUENCES TO <애플리케이션_사용자>;
```

---

## 4. 테이블 생성

코드에서 파악한 테이블 구조입니다. **기존 DB에서 덤프한 SQL이 있다면 그것을 우선 사용하세요.**

### 4.1 lunch.restaurant

```sql
CREATE TABLE lunch.restaurant (
    management_number VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(255),
    sido VARCHAR(255),
    sigungu VARCHAR(255),
    dongmyun VARCHAR(255),
    ri VARCHAR(255),
    road VARCHAR(255),
    building_number VARCHAR(255),
    address VARCHAR(255),
    location geometry(Point, 4326) NOT NULL,  -- PostGIS 타입
    status VARCHAR(50) NOT NULL,  -- OPEN, CLOSED, UNKNOWN
    h3_indices TEXT[] DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 공간 인덱스 (PostGIS)
CREATE INDEX idx_restaurant_location ON lunch.restaurant USING GIST(location);

-- H3 인덱스 배열 검색용
CREATE INDEX idx_restaurant_h3_indices ON lunch.restaurant USING GIN(h3_indices);

-- 상태별 검색용
CREATE INDEX idx_restaurant_status ON lunch.restaurant(status);
```

### 4.2 open_data_cloud.seoul_restaurant

```sql
CREATE TABLE open_data_cloud.seoul_restaurant (
    id BIGSERIAL PRIMARY KEY,
    management_number VARCHAR(255) NOT NULL,
    open_self_team_code VARCHAR(255),
    approval_date DATE,
    approval_cancel_date DATE,
    trade_state_code VARCHAR(50) NOT NULL,
    trade_state_name VARCHAR(255) NOT NULL,
    detail_trade_state_code VARCHAR(50) NOT NULL,
    detail_trade_state_name VARCHAR(255) NOT NULL,
    close_date DATE,
    pause_start_date DATE,
    pause_end_date DATE,
    reopen_date DATE,
    site_tel VARCHAR(255),
    site_area VARCHAR(255),
    site_post_no VARCHAR(20) NOT NULL,
    site_whole_address TEXT,
    road_whole_address TEXT,
    road_post_no VARCHAR(20) NOT NULL,
    business_place_name VARCHAR(255),
    last_modified_timestamp TIMESTAMP,
    update_type VARCHAR(50),
    update_date TIMESTAMP,
    business_type VARCHAR(255),
    x_coordinate DOUBLE PRECISION,
    y_coordinate DOUBLE PRECISION,
    sanitary_business_type VARCHAR(255),
    male_employee_count INTEGER,
    female_employee_count INTEGER,
    trade_surrounding_category VARCHAR(255),
    grade_category VARCHAR(255),
    water_supply_facility VARCHAR(255),
    total_employees INTEGER,
    headquarters_employees INTEGER,
    factory_office_workers INTEGER,
    factory_sales_workers INTEGER,
    factory_production_workers INTEGER,
    building_ownership_category VARCHAR(255),
    security_deposit DOUBLE PRECISION,
    monthly_rent DOUBLE PRECISION,
    multi_use_business VARCHAR(255),
    total_facility_size DOUBLE PRECISION,
    traditional_business_number VARCHAR(255),
    traditional_main_dish VARCHAR(255),
    homepage VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 유니크 인덱스
CREATE UNIQUE INDEX idx_restaurant_management_number ON open_data_cloud.seoul_restaurant(management_number);

-- 검색용 인덱스
CREATE INDEX idx_seoul_restaurant_trade_state ON open_data_cloud.seoul_restaurant(trade_state_code);
CREATE INDEX idx_seoul_restaurant_approval_date ON open_data_cloud.seoul_restaurant(approval_date);
```

### 4.3 lunch.users

```sql
CREATE TABLE lunch.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255),
    nickname VARCHAR(255) UNIQUE NOT NULL,
    login_id VARCHAR(255),
    password VARCHAR(255),
    last_login_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 검색 인덱스
CREATE INDEX idx_users_email ON lunch.users(email);
CREATE INDEX idx_users_login_id ON lunch.users(login_id);
```

### 4.4 lunch.user_identity

```sql
CREATE TABLE lunch.user_identity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES lunch.users(id),
    provider VARCHAR(50) NOT NULL,  -- AZURE 등
    subject VARCHAR(255) NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 유니크 제약
ALTER TABLE lunch.user_identity
    ADD CONSTRAINT uk_user_identities_provider_subject
    UNIQUE (provider, subject);

-- 인덱스
CREATE INDEX idx_user_identities_user_id ON lunch.user_identity(user_id);
```

### 4.5 lunch.review

```sql
CREATE TABLE lunch.review (
    id BIGSERIAL PRIMARY KEY,
    restaurant_management_number VARCHAR(255) NOT NULL,
    reviewer_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT NOT NULL,
    image_urls JSONB DEFAULT '[]',
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',  -- CREATED, DELETED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 인덱스
CREATE INDEX idx_review_restaurant ON lunch.review(restaurant_management_number);
CREATE INDEX idx_review_reviewer ON lunch.review(reviewer_id);
CREATE INDEX idx_review_status ON lunch.review(status);
CREATE INDEX idx_review_created_at ON lunch.review(created_at DESC);

-- 외래 키
ALTER TABLE lunch.review
    ADD CONSTRAINT fk_review_restaurant
    FOREIGN KEY (restaurant_management_number)
    REFERENCES lunch.restaurant(management_number);

ALTER TABLE lunch.review
    ADD CONSTRAINT fk_review_user
    FOREIGN KEY (reviewer_id)
    REFERENCES lunch.users(id);
```

### 4.6 lunch.image

```sql
CREATE TABLE lunch.image (
    id BIGSERIAL PRIMARY KEY,
    name UUID NOT NULL,
    user_id BIGINT NOT NULL,
    context VARCHAR(50) NOT NULL,  -- Enum: ImageContext
    object_key VARCHAR(500) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attached_source VARCHAR(255),
    attached_at TIMESTAMP WITH TIME ZONE
);

-- 유니크 인덱스
CREATE UNIQUE INDEX idx_image_name ON lunch.image(name);

-- 검색 인덱스
CREATE INDEX idx_image_user_id ON lunch.image(user_id);
CREATE INDEX idx_image_context ON lunch.image(context);
CREATE INDEX idx_image_attached_source ON lunch.image(attached_source);

-- 외래 키
ALTER TABLE lunch.image
    ADD CONSTRAINT fk_image_user
    FOREIGN KEY (user_id)
    REFERENCES lunch.users(id);
```

### 4.7 lunch.token

```sql
CREATE TABLE lunch.token (
    id BIGSERIAL PRIMARY KEY,
    family_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(500) UNIQUE NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    generation INTEGER NOT NULL DEFAULT 0
);

-- 인덱스
CREATE INDEX idx_token_refresh_token ON lunch.token(refresh_token);
CREATE INDEX idx_token_family_id ON lunch.token(family_id);
CREATE INDEX idx_token_user_id ON lunch.token(user_id);
CREATE INDEX idx_token_expires_at ON lunch.token(expires_at);

-- 외래 키
ALTER TABLE lunch.token
    ADD CONSTRAINT fk_token_user
    FOREIGN KEY (user_id)
    REFERENCES lunch.users(id);
```

### 4.8 lunch.authorization_session

```sql
CREATE TABLE lunch.authorization_session (
    code UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,  -- AZURE 등
    subject VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(2048) NOT NULL,
    state UUID NOT NULL,
    name VARCHAR(255),  -- UserProfile 임베디드
    email VARCHAR(255),  -- UserProfile 임베디드
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

-- 인덱스
CREATE INDEX idx_auth_session_state ON lunch.authorization_session(state);
CREATE INDEX idx_auth_session_expires_at ON lunch.authorization_session(expires_at);
```

### 4.9 lunch.oauth2_authorization_request

```sql
CREATE TABLE lunch.oauth2_authorization_request (
    state VARCHAR(255) PRIMARY KEY,
    authorization_uri VARCHAR(2048) NOT NULL,
    grant_type VARCHAR(50) NOT NULL,
    response_type VARCHAR(50) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(2048) NOT NULL,
    scopes JSONB NOT NULL,
    additional_parameters JSONB DEFAULT '{}',
    authorization_request_uri VARCHAR(2048) NOT NULL,
    attributes JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_oauth2_auth_request_created_at ON lunch.oauth2_authorization_request(created_at);
```

---

## 5. 인덱스 및 제약조건

위 테이블 생성 스크립트에 이미 포함되어 있지만, 별도로 추가해야 할 경우:

```sql
-- 복합 인덱스 예시 (필요시 추가)
CREATE INDEX idx_review_restaurant_status ON lunch.review(restaurant_management_number, status);
CREATE INDEX idx_restaurant_status_location ON lunch.restaurant(status) WHERE status = 'OPEN';  -- 부분 인덱스
```

---

## 6. CDC (Change Data Capture) 설정

### 6.1 PostgreSQL 서버 설정

`postgresql.conf` 파일 수정 (슈퍼유저 권한 필요):

```ini
# WAL 설정
wal_level = logical
max_replication_slots = 4
max_wal_senders = 4
```

설정 후 PostgreSQL 재시작:
```bash
sudo systemctl restart postgresql
# 또는 Docker
docker-compose restart postgres
```

### 6.2 Replication Slot 생성

```sql
-- 슈퍼유저 또는 REPLICATION 권한을 가진 사용자로 실행
SELECT pg_create_logical_replication_slot('seoul_restaurant', 'wal2json');

-- 생성 확인
SELECT slot_name, plugin, slot_type, active
FROM pg_replication_slots
WHERE slot_name = 'seoul_restaurant';
```

**출력 예시:**
```
 slot_name        | plugin   | slot_type | active
------------------+----------+-----------+--------
 seoul_restaurant | wal2json | logical   | f
```

### 6.3 애플리케이션 사용자에게 Replication 권한 부여

```sql
-- 데이터베이스 레벨에서 replication 권한 부여
ALTER USER <애플리케이션_사용자> WITH REPLICATION;

-- 또는 pg_hba.conf에 추가 (슈퍼유저 권한 필요)
-- host    replication    <애플리케이션_사용자>    <IP주소>/32    md5
```

### 6.4 CDC 동작 확인

애플리케이션이 시작되면 `SeoulRestaurantCdcListener`가 자동으로 Replication Stream에 연결됩니다.

```kotlin
// src/main/kotlin/com/usktea/lunch/listener/SeoulRestaurantCdcListener.kt:18
override val slotName: String = "seoul_restaurant"
```

테스트:
```sql
-- seoul_restaurant 테이블에 데이터 INSERT
INSERT INTO open_data_cloud.seoul_restaurant (management_number, trade_state_code, ...)
VALUES ('TEST-001', '01', ...);

-- 애플리케이션 로그에서 CDC 이벤트 처리 확인
-- RestaurantEventService.insertRestaurantByEvents()가 호출되어야 함
```

---

## 7. 애플리케이션 설정 업데이트

### 7.1 환경 변수 설정

`.env` 파일 또는 환경 변수:

```bash
# 새 데이터베이스 접속 정보
DATABASE_URL=jdbc:postgresql://<새_호스트>:<포트>/<새_데이터베이스명>
DATABASE_USERNAME=<사용자명>
DATABASE_PASSWORD=<비밀번호>

# 기존 설정 유지
AZURE_CLIENT_ID=...
AZURE_CLIENT_SECRET=...
SEOUL_OPEN_DATA_APP_KEY=...
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
AWS_ACCESS_KEY=...
AWS_SECRET_KEY=...
JWT_PRIVATE_KEY=...
JWT_PUBLIC_KEY=...
```

### 7.2 application.yml 확인

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: none  # 반드시 none으로 유지
```

---

## 8. 검증

### 8.1 스키마 확인

```sql
-- 스키마 존재 확인
\dn

-- 테이블 목록
\dt lunch.*
\dt open_data_cloud.*

-- 특정 테이블 구조 확인
\d+ lunch.restaurant
\d+ open_data_cloud.seoul_restaurant

-- 인덱스 확인
\di+ lunch.*
\di+ open_data_cloud.*

-- 외래 키 확인
SELECT
    tc.table_schema,
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_schema AS foreign_table_schema,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
  AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
  AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema IN ('lunch', 'open_data_cloud');
```

### 8.2 확장 기능 확인

```sql
-- PostGIS 확인
SELECT PostGIS_version();

-- wal2json 확인
SELECT * FROM pg_available_extensions WHERE name = 'wal2json';

-- Replication Slot 확인
SELECT * FROM pg_replication_slots WHERE slot_name = 'seoul_restaurant';
```

### 8.3 애플리케이션 실행 테스트

```bash
# 로컬에서 실행
./gradlew bootRun

# 또는 Docker
docker-compose up -d
docker logs -f lunch
```

**확인 사항:**
- ✅ 애플리케이션이 정상 시작되는지
- ✅ CDC 리스너가 Replication Slot에 연결되는지
- ✅ Cron 작업이 정상 동작하는지 (매일 04:00 AM KST)
- ✅ 서울 열린데이터광장 API 크롤링이 동작하는지

### 8.4 CDC 동작 테스트

```sql
-- 테스트 데이터 INSERT
INSERT INTO open_data_cloud.seoul_restaurant
(management_number, trade_state_code, trade_state_name, detail_trade_state_code,
 detail_trade_state_name, site_post_no, road_post_no)
VALUES
('TEST-2024-001', '01', '영업', '01', '정상영업', '12345', '12345');

-- lunch.restaurant 테이블에 자동으로 생성되었는지 확인
SELECT * FROM lunch.restaurant WHERE management_number LIKE 'TEST-%';
```

---

## 9. 데이터 마이그레이션 (선택사항)

스키마만 복사하는 게 아니라 **기존 데이터도 복사**해야 한다면:

### 9.1 데이터 덤프

```bash
# 데이터만 추출 (INSERT 문으로)
pg_dump -h <기존_호스트> \
        -U <사용자명> \
        -d <데이터베이스명> \
        --data-only \
        --column-inserts \
        --schema=lunch \
        --schema=open_data_cloud \
        -f data_dump.sql

# 또는 COPY 형식 (더 빠름)
pg_dump -h <기존_호스트> \
        -U <사용자명> \
        -d <데이터베이스명> \
        --data-only \
        --schema=lunch \
        --schema=open_data_cloud \
        -f data_dump_copy.sql
```

### 9.2 데이터 복원

```bash
# 새 DB에 복원
psql -h <새_호스트> \
     -U <사용자명> \
     -d <새_데이터베이스명> \
     -f data_dump.sql
```

### 9.3 시퀀스 값 재설정

```sql
-- SERIAL 컬럼의 시퀀스를 현재 최대값+1로 재설정
SELECT setval('lunch.users_id_seq', (SELECT MAX(id) FROM lunch.users));
SELECT setval('lunch.review_id_seq', (SELECT MAX(id) FROM lunch.review));
SELECT setval('lunch.image_id_seq', (SELECT MAX(id) FROM lunch.image));
SELECT setval('lunch.token_id_seq', (SELECT MAX(id) FROM lunch.token));
SELECT setval('lunch.user_identity_id_seq', (SELECT MAX(id) FROM lunch.user_identity));
SELECT setval('open_data_cloud.seoul_restaurant_id_seq', (SELECT MAX(id) FROM open_data_cloud.seoul_restaurant));
```

---

## 10. Cron 작업

코드에서 확인된 스케줄 작업:

```kotlin
// src/main/kotlin/com/usktea/lunch/service/crawler/SeoulRestaurantEntityCrawlerService.kt:25
@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
```

**작업 내용:**
- 매일 04:00 AM (KST) 실행
- 서울시 일반음식점 신규 데이터 크롤링
- `SeoulRestaurantEntityCrawlerService.downloadNewRestaurants()` 호출

**데이터베이스 설정은 불필요** - 애플리케이션 레벨 스케줄러이므로 DB 마이그레이션 시 별도 작업 없음.

---

## 11. 트러블슈팅

### 문제 1: wal2json 플러그인을 찾을 수 없음

**증상:**
```
ERROR: could not access file "wal2json": No such file or directory
```

**해결:**
```bash
# Ubuntu/Debian
sudo apt-get install postgresql-15-wal2json

# Docker 사용 시 이미지 변경
# debezium/postgres 또는 postgis/postgis 이미지 사용
```

### 문제 2: Replication 권한 부족

**증상:**
```
ERROR: must be superuser or replication role to use replication slots
```

**해결:**
```sql
ALTER USER <사용자명> WITH REPLICATION;
```

### 문제 3: PostGIS geometry 타입 오류

**증상:**
```
ERROR: type "geometry" does not exist
```

**해결:**
```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 문제 4: CDC 리스너가 연결되지 않음

**확인사항:**
```sql
-- WAL 레벨 확인
SHOW wal_level;  -- logical이어야 함

-- Replication Slot 확인
SELECT * FROM pg_replication_slots;

-- 서버 설정 확인
SHOW max_replication_slots;  -- 최소 1 이상
SHOW max_wal_senders;  -- 최소 1 이상
```

---

## 12. 체크리스트

마이그레이션 전 확인:
- [ ] 기존 DB 접속 가능
- [ ] 새 DB 생성 완료
- [ ] PostgreSQL 버전 호환성 확인 (9.4+)
- [ ] 슈퍼유저 또는 충분한 권한 보유

스키마 마이그레이션:
- [ ] PostGIS 확장 설치
- [ ] wal2json 플러그인 설치
- [ ] 스키마 생성 (lunch, open_data_cloud)
- [ ] 모든 테이블 생성
- [ ] 인덱스 생성 확인
- [ ] 외래 키 제약 확인

CDC 설정:
- [ ] wal_level = logical 설정
- [ ] max_replication_slots 설정 (최소 4)
- [ ] max_wal_senders 설정 (최소 4)
- [ ] PostgreSQL 재시작
- [ ] Replication Slot 생성 (seoul_restaurant)
- [ ] 애플리케이션 사용자에게 REPLICATION 권한 부여

애플리케이션 설정:
- [ ] DATABASE_URL 업데이트
- [ ] DATABASE_USERNAME 업데이트
- [ ] DATABASE_PASSWORD 업데이트
- [ ] 애플리케이션 실행 테스트
- [ ] CDC 리스너 연결 확인
- [ ] 크롤러 동작 확인

데이터 마이그레이션 (선택):
- [ ] 기존 데이터 덤프
- [ ] 새 DB로 데이터 복원
- [ ] 시퀀스 값 재설정
- [ ] 데이터 무결성 검증

---

## 참고 자료

- [PostgreSQL Logical Replication](https://www.postgresql.org/docs/current/logical-replication.html)
- [wal2json GitHub](https://github.com/eulerto/wal2json)
- [PostGIS Documentation](https://postgis.net/documentation/)
- [Spring Boot JPA Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.jpa-and-spring-data)