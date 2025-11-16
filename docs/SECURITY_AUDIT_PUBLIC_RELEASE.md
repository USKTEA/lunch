# 보안 감사 리포트 - Public Repository 공개 전 검토

**작성일:** 2025-11-17
**검토 대상:** lunch 프로젝트 (Spring Boot + React)
**목적:** GitHub Public Repository 공개 가능 여부 판단

---

## 📋 요약

### ⚠️ **결론: 조건부 Public 공개 가능**

**해결해야 할 문제:**
1. ⚠️ 프론트엔드에 **프로덕션 API URL이 하드코딩**되어 있음
2. ⚠️ `.env.example` 파일 생성 필요
3. ⚠️ README.md 환경 변수 가이드 추가 필요

**중요:** `.env` 파일은 **로컬에만 존재**하며 Git 추적되지 않음

**긍정적인 부분:**
- ✅ `.env` 파일이 `.gitignore`에 포함되어 Git 추적 안 됨
- ✅ Git 히스토리에 민감 정보 커밋 이력 없음
- ✅ 소스 코드에 직접 하드코딩된 비밀키 없음
- ✅ `application.yml`은 환경 변수 참조 방식 사용 (안전)

---

## 🔍 상세 검토 결과

### 1. 민감 정보 노출 현황

#### ❌ **Critical: `.env` 파일 (루트 디렉토리)**

**위치:** `/Users/suktae/work/personal/lunch/.env`

**노출된 정보 (예시 - 실제 값은 마스킹됨):**
```
DATABASE_URL=jdbc:postgresql://[RDS_ENDPOINT]:5432/lunch
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=[REDACTED]

AZURE_CLIENT_ID=[REDACTED]
AZURE_CLIENT_SECRET=[REDACTED]
AZURE_ISSUER_URI=https://login.microsoftonline.com/[TENANT_ID]/v2.0

SEOUL_OPEN_DATA_APP_KEY=[REDACTED]

NAVER_CLIENT_ID=[REDACTED]
NAVER_CLIENT_SECRET=[REDACTED]

AWS_ACCESS_KEY=[REDACTED]
AWS_SECRET_KEY=[REDACTED]

CLOUDFRONT_KEY_PAIR_ID=[REDACTED]
CLOUDFRONT_PRIVATE_KEY=(2048-bit RSA Private Key - REDACTED)

JWT_PRIVATE_KEY=(2048-bit RSA Private Key - REDACTED)
JWT_PUBLIC_KEY=(2048-bit RSA Public Key)
```

**위험도:** 🚨 **CRITICAL**

**영향:**
- ✅ Git에 추적되지 않음 (`.gitignore`에 포함)
- ❌ 하지만 로컬에 평문으로 존재
- ❌ 실수로 커밋될 위험 존재

---

#### ⚠️ **Medium: `web/.env` 파일**

**위치:** `/Users/suktae/work/personal/lunch/web/.env`

**노출된 정보:**
```
REACT_APP_NAVER_MAP_CLIENT_ID=[REDACTED]
REACT_APP_API_BASE_URL=http://localhost:8080
```

**위험도:** ⚠️ **MEDIUM**

**영향:**
- ✅ Git에 추적되지 않음
- ⚠️ 네이버 지도 API 클라이언트 ID는 **공개 가능**하지만 권장하지 않음
- ⚠️ API URL이 `localhost`이므로 프로덕션 설정과 분리 필요

---

#### ⚠️ **Medium: 프론트엔드 하드코딩된 URL**

**위치:** `web/src/service/ApiService.js:9`

```javascript
baseURL: "https://api.dev-htbeyondcloud.com",
```

**위험도:** ⚠️ **MEDIUM**

**문제점:**
- 프로덕션 API 도메인이 하드코딩됨
- `dev-htbeyondcloud.com` 도메인 노출
- 환경별 설정 분리 안 됨

**추가 노출 위치:**
- `web/src/stores/AuthStore.js:xxx` - OAuth 리다이렉트 URL
- `web/src/config/constants.js` - CloudFront 도메인

---

### 2. Git 히스토리 분석

**검토 결과:** ✅ **안전**

```bash
# 커밋 이력
aef9876 Initial commit (README.md만 포함)
235b16d 리포지토리 마이그레이션 (소스 코드 추가)
```

**확인 사항:**
- ✅ `.env` 파일이 Git에 커밋된 적 없음
- ✅ 민감 정보가 포함된 설정 파일 커밋 이력 없음
- ✅ 소스 코드만 커밋됨

---

### 3. 소스 코드 내 하드코딩 검사

**검토 결과:** ✅ **안전**

- ✅ Kotlin 백엔드 코드: 모든 민감 정보가 환경 변수 참조
- ✅ `application.yml`: `${DATABASE_URL}` 등 환경 변수 사용
- ✅ 주석에 비밀 정보 없음
- ⚠️ 프론트엔드: API URL 하드코딩 (위에서 언급)

---

### 4. 공개된 정보 분석

#### ✅ **안전하게 공개 가능한 정보**

1. **도메인 정보**
   - `api.dev-htbeyondcloud.com` - 토이 프로젝트 도메인
   - `static.dev-htbeyondcloud.com` - CloudFront 도메인
   - **판단:** 문제없음 (이미 공개된 DNS)

2. **AWS S3 버킷 이름**
   - `htbeyond-lunch`
   - **판단:** 버킷이 public이 아니면 문제없음

3. **Docker Hub 이미지**
   - `suktaekim/lunch:latest`
   - **판단:** Private repository라면 문제없음

4. **네이버 지도 API 클라이언트 ID**
   - `[REDACTED]`
   - **판단:** 클라이언트 ID는 공개 가능하지만 권장하지 않음

---

## 🎯 위험 요소 평가

### Critical (즉시 조치 필요)

1. **AWS Access Key 노출 위험**
   - `.env` 파일에 `AWS_ACCESS_KEY`, `AWS_SECRET_KEY` 존재
   - Git에 실수로 커밋될 경우 **AWS 계정 탈취** 가능
   - **권장:** AWS Systems Manager Parameter Store 또는 Secrets Manager 사용

2. **Azure OAuth 비밀키 노출 위험**
   - `AZURE_CLIENT_SECRET` 노출 시 **OAuth 인증 우회** 가능
   - **권장:** Azure Key Vault 사용

3. **데이터베이스 비밀번호 노출**
   - `DATABASE_PASSWORD=[REDACTED]`
   - RDS 접근 권한 탈취 가능
   - **권장:** AWS Secrets Manager 사용

4. **JWT Private Key 노출**
   - JWT 서명 키 노출 시 **모든 사용자 인증 토큰 위조** 가능
   - **권장:** AWS KMS 또는 별도 키 관리 시스템

### High (우선 조치 권장)

1. **Naver API Secret 노출**
   - 네이버 API 쿼터 도용 가능
   - **영향:** 요금 폭탄, API 차단

2. **CloudFront Private Key 노출**
   - Signed Cookie/URL 위조 가능
   - **영향:** 이미지 무단 접근

### Medium (점진적 개선)

1. **프론트엔드 API URL 하드코딩**
   - 환경별 설정 분리 필요
   - **권장:** `process.env.REACT_APP_API_BASE_URL` 사용

2. **서울 열린데이터광장 API 키**
   - 공공 API이므로 영향 제한적
   - 하지만 노출 권장하지 않음

---

## ✅ Public 공개 전 필수 조치사항

### 1. 즉시 삭제해야 할 파일 (절대 커밋 금지)

```bash
# 삭제 또는 Git 추적에서 영구 제외
.env
web/.env
```

**확인 방법:**
```bash
git check-ignore .env
# 출력: .env (이미 .gitignore에 포함됨 - OK)

git ls-files | grep "\.env"
# 출력 없어야 함 (현재: OK)
```

---

### 2. 프론트엔드 하드코딩 제거

#### ⚠️ **문제 파일들:**

1. **`web/src/service/ApiService.js`**
   ```javascript
   // ❌ 현재
   baseURL: "https://api.dev-htbeyondcloud.com",

   // ✅ 변경 후
   baseURL: process.env.REACT_APP_API_BASE_URL || "http://localhost:8080",
   ```

2. **`web/src/stores/AuthStore.js`**
   ```javascript
   // ❌ 현재
   window.location.href = `https://api.dev-htbeyondcloud.com${provider.authorizationUri}?...`;

   // ✅ 변경 후
   const apiBaseUrl = process.env.REACT_APP_API_BASE_URL || window.location.origin;
   window.location.href = `${apiBaseUrl}${provider.authorizationUri}?...`;
   ```

3. **`web/src/config/constants.js`**
   ```javascript
   // ❌ 현재
   export const CLOUDFRONT_DOMAIN = 'static.dev-htbeyondcloud.com';

   // ✅ 변경 후
   export const CLOUDFRONT_DOMAIN = process.env.REACT_APP_CLOUDFRONT_DOMAIN || 'static.dev-htbeyondcloud.com';
   ```

---

### 3. `.env.example` 파일 생성 (Git 추적 가능)

**생성 위치:** 루트 디렉토리

```bash
# .env.example
# 이 파일을 복사해서 .env로 만들고 실제 값을 입력하세요

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/lunch
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password_here

# Azure OAuth
AZURE_CLIENT_ID=your_client_id
AZURE_CLIENT_SECRET=your_client_secret
AZURE_ISSUER_URI=https://login.microsoftonline.com/your_tenant_id/v2.0

# Seoul Open Data
SEOUL_OPEN_DATA_APP_KEY=your_api_key

# Naver API
NAVER_CLIENT_ID=your_client_id
NAVER_CLIENT_SECRET=your_client_secret

# AWS
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key

# CloudFront
CLOUDFRONT_KEY_PAIR_ID=your_key_pair_id
CLOUDFRONT_PRIVATE_KEY=your_private_key

# JWT Keys
JWT_PRIVATE_KEY=your_private_key
JWT_PUBLIC_KEY=your_public_key
```

**프론트엔드용:**
```bash
# web/.env.example
REACT_APP_NAVER_MAP_CLIENT_ID=your_naver_map_client_id
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_CLOUDFRONT_DOMAIN=your_cloudfront_domain
```

---

### 4. README.md 업데이트

**추가할 섹션:**

```markdown
## 환경 변수 설정

이 프로젝트는 민감한 정보를 환경 변수로 관리합니다.

### 백엔드 설정

1. 루트 디렉토리에 `.env` 파일 생성
2. `.env.example` 파일을 참고하여 값 입력

### 프론트엔드 설정

1. `web/.env` 파일 생성
2. `web/.env.example` 파일을 참고하여 값 입력

### 필요한 API 키

- **Azure AD OAuth 2.0**: [Azure Portal](https://portal.azure.com/)에서 발급
- **서울 열린데이터광장**: [서울 열린데이터광장](https://data.seoul.go.kr/)에서 발급
- **네이버 지도 API**: [Naver Cloud Platform](https://www.ncloud.com/)에서 발급
- **AWS**: AWS IAM에서 Access Key 발급

### ⚠️ 주의사항

- **절대 `.env` 파일을 Git에 커밋하지 마세요!**
- 프로덕션 환경에서는 AWS Secrets Manager, Azure Key Vault 사용 권장
```

---

### 5. .gitignore 검증

**현재 상태:** ✅ **양호**

```gitignore
# 이미 포함되어 있음
.env
.env.prod
**/.env
```

**추가 권장:**
```gitignore
# 추가할 내용
*.env.local
*.env.*.local
.env.development
.env.production

# AWS CLI credentials
.aws/

# Terraform state files (만약 사용한다면)
*.tfstate
*.tfstate.backup
```

---

### 6. Git 히스토리 정리 (선택사항)

현재는 민감 정보가 Git 히스토리에 없으므로 **불필요**합니다.

만약 과거에 커밋했다면:
```bash
# BFG Repo-Cleaner 사용
bfg --delete-files .env
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

---

## 🛡️ 보안 모범 사례

### 1. AWS 보안 강화

```bash
# AWS Secrets Manager 사용 예시 (권장)
aws secretsmanager create-secret \
  --name lunch/db/password \
  --secret-string "your_actual_password"

# application.yml에서 참조
spring:
  cloud:
    aws:
      secretsmanager:
        enabled: true
  datasource:
    password: ${lunch/db/password}
```

### 2. GitHub Secrets 사용 (CI/CD)

```yaml
# .github/workflows/deploy.yml
- name: Deploy
  env:
    DATABASE_PASSWORD: ${{ secrets.DATABASE_PASSWORD }}
    AWS_ACCESS_KEY: ${{ secrets.AWS_ACCESS_KEY }}
```

### 3. Docker 이미지 빌드 시 주의

```dockerfile
# ❌ 절대 이렇게 하지 마세요
COPY .env /app/.env

# ✅ 환경 변수는 런타임에 주입
ENV DATABASE_URL=${DATABASE_URL}
```

### 4. API 키 로테이션 주기 설정

- AWS Access Key: **90일마다 로테이션**
- Azure Client Secret: **180일마다 갱신**
- JWT Private Key: **1년마다 교체**

---

## 📊 보안 체크리스트

### Public 공개 전

- [ ] `.env` 파일이 Git에 없는지 확인 (`git ls-files | grep .env`)
- [ ] `.gitignore`에 `.env` 포함 확인
- [ ] `.env.example` 파일 생성
- [ ] `web/.env.example` 파일 생성
- [ ] 프론트엔드 하드코딩 URL 제거
- [ ] README.md에 환경 변수 설정 가이드 추가
- [ ] 소스 코드에서 "password", "secret", "key" 문자열 재검색
- [ ] Git 히스토리 재검증

### 공개 후

- [ ] GitHub Repository Settings → Secrets 설정 (CI/CD용)
- [ ] AWS IAM 정책 최소 권한 원칙 적용
- [ ] CloudFront 접근 로그 모니터링 설정
- [ ] 민감 정보 노출 모니터링 (GitGuardian, TruffleHog)

---

## 🔧 자동화 도구 추천

### 1. Git Hooks (pre-commit)

```bash
# .git/hooks/pre-commit
#!/bin/bash
if git diff --cached --name-only | grep -E "\.env$"; then
  echo "❌ .env 파일을 커밋하려고 시도했습니다!"
  exit 1
fi
```

### 2. Secrets 스캐너

```bash
# TruffleHog 사용
pip install truffleHog
trufflehog --regex --entropy=True .

# GitGuardian CLI
ggshield secret scan repo .
```

---

## 🚦 최종 권장사항

### 🔴 **즉시 조치 (Public 공개 전 필수)**

1. ✅ `.env` 파일이 Git에 추적되지 않는지 재확인
2. ⚠️ 프론트엔드 하드코딩 URL을 환경 변수로 변경
3. ⚠️ `.env.example` 생성 및 README 업데이트

### 🟡 **우선 조치 (1주일 내)**

1. AWS Secrets Manager로 민감 정보 이전
2. API 키 로테이션 정책 수립
3. GitHub Secrets 설정 (CI/CD용)

### 🟢 **점진적 개선 (1개월 내)**

1. 프론트엔드 환경별 빌드 설정 분리
2. 보안 모니터링 도구 도입
3. 정기 보안 감사 프로세스 수립

---

## 📌 결론

**현재 상태:** ⚠️ **조건부 공개 가능**

**조건:**
1. ✅ `.env` 파일이 Git에 추적되지 않음 (현재 OK)
2. ⚠️ 프론트엔드 하드코딩 제거 필요
3. ⚠️ `.env.example` 생성 및 README 업데이트 필요

**위 3가지 조치만 완료하면 Public 공개 가능합니다.**

**장기적으로:**
- AWS Secrets Manager 도입 권장
- API 키 로테이션 자동화
- 보안 모니터링 설정

---

**검토자:** Claude Code
**다음 검토 예정일:** 2025-12-17 (1개월 후)
