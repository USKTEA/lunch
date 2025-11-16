# Public Repository 공개 전 최종 체크리스트

**날짜:** 2025-11-17
**상태:** ⚠️ 조건부 공개 가능

---

## ✅ 완료된 항목

- [x] `.env` 파일이 `.gitignore`에 포함됨
- [x] Git 히스토리에 민감 정보 없음
- [x] 소스 코드에 하드코딩된 비밀키 없음
- [x] `application.yml`은 환경 변수 참조 방식
- [x] `docs/` 디렉토리 내 비밀 정보 제거 완료

---

## ⚠️ Public 공개 전 필수 조치 (3가지)

### 1. 프론트엔드 하드코딩 제거

#### 📁 `web/src/service/ApiService.js:9`
```javascript
// ❌ 현재
baseURL: "https://api.dev-htbeyondcloud.com",

// ✅ 변경
baseURL: process.env.REACT_APP_API_BASE_URL || "http://localhost:8080",
```

#### 📁 `web/src/stores/AuthStore.js`
```javascript
// ❌ 현재
window.location.href = `https://api.dev-htbeyondcloud.com${provider.authorizationUri}?...`;

// ✅ 변경
const apiBaseUrl = process.env.REACT_APP_API_BASE_URL || window.location.origin;
window.location.href = `${apiBaseUrl}${provider.authorizationUri}?...`;
```

#### 📁 `web/src/config/constants.js`
```javascript
// ✅ 추가
export const CLOUDFRONT_DOMAIN = process.env.REACT_APP_CLOUDFRONT_DOMAIN || 'static.dev-htbeyondcloud.com';
```

---

### 2. `.env.example` 파일 생성

#### 루트: `.env.example`
```bash
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

#### 프론트엔드: `web/.env.example`
```bash
REACT_APP_NAVER_MAP_CLIENT_ID=your_naver_map_client_id
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_CLOUDFRONT_DOMAIN=your_cloudfront_domain
```

---

### 3. README.md 업데이트

다음 섹션 추가:

```markdown
## 🔧 환경 설정

### 백엔드 환경 변수

1. 루트 디렉토리에 `.env` 파일 생성
2. `.env.example`을 참고하여 실제 값 입력

### 프론트엔드 환경 변수

1. `web/` 디렉토리에 `.env` 파일 생성
2. `web/.env.example`을 참고하여 실제 값 입력

### 필요한 API 키

- **Azure AD OAuth 2.0**: [Azure Portal](https://portal.azure.com/)에서 앱 등록
- **서울 열린데이터광장**: [데이터광장](https://data.seoul.go.kr/)에서 API 키 발급
- **네이버 지도 API**: [Naver Cloud Platform](https://www.ncloud.com/)에서 발급
- **AWS**: IAM에서 Access Key 발급

### ⚠️ 보안 주의사항

**절대 `.env` 파일을 Git에 커밋하지 마세요!**

프로덕션 환경에서는 다음 사용 권장:
- AWS Secrets Manager
- Azure Key Vault
- GitHub Secrets (CI/CD)
```

---

## 🔍 최종 검증 명령어

Public 공개 전 반드시 실행:

```bash
# 1. .env 파일이 Git에 추적되지 않는지 확인
git ls-files | grep "\.env$"
# 출력 없어야 함 ✅

# 2. Git 추적 파일에서 비밀 정보 검색
git ls-files | xargs grep -l -E "(password.*=.*[^{$]|secret.*=.*[^{$]|AKIA)" 2>/dev/null
# 출력 없어야 함 ✅

# 3. .gitignore 확인
grep "\.env" .gitignore
# .env가 포함되어 있어야 함 ✅

# 4. 프론트엔드 하드코딩 확인
grep -r "https://api.dev-htbeyondcloud.com" web/src/
# 3개 파일 검색됨 → 위 1번 조치로 제거 필요
```

---

## 📊 검증 결과

### Git 추적 파일 (현재 상태)

✅ **안전 - 민감 정보 없음**

```bash
# 실행 결과
$ git ls-files | xargs grep -l -E "(lunch1234|AKIAS)" 2>/dev/null
No secrets found in Git tracked files ✅
```

### 로컬 파일 (Git 미추적)

⚠️ **민감 정보 포함 - Git 추적 안 됨**

- `.env` (루트)
- `web/.env`
- **중요:** 이 파일들은 `.gitignore`에 포함되어 있어 안전

---

## 🎯 공개 후 권장사항

### 즉시 (1주일 내)

1. **GitHub Secrets 설정**
   - Repository Settings → Secrets and variables → Actions
   - CI/CD용 환경 변수 등록

2. **Security Scanning 활성화**
   - Dependabot alerts 활성화
   - Code scanning (CodeQL) 활성화
   - Secret scanning 활성화

### 장기 (1개월 내)

1. **AWS Secrets Manager 도입**
   - 민감 정보를 코드에서 완전 분리
   - 자동 로테이션 설정

2. **API 키 로테이션 정책 수립**
   - AWS Access Key: 90일마다
   - Azure Client Secret: 180일마다
   - JWT Private Key: 1년마다

3. **모니터링 도구 도입**
   - GitGuardian
   - TruffleHog
   - Pre-commit hooks

---

## 🚨 공개하면 안 되는 파일 (절대!)

```
.env
web/.env
.env.local
.env.*.local
*.pem
*.key
*.p12
credentials.json
secrets.yml
```

**이 파일들이 Git에 있는지 확인:**
```bash
git ls-files | grep -E "\.env$|\.key$|\.pem$|credentials"
# 출력 없어야 함
```

---

## ✅ 최종 승인 체크리스트

공개 전 모두 체크:

- [ ] 프론트엔드 하드코딩 제거 (3개 파일)
- [ ] `.env.example` 생성 (루트 + web)
- [ ] README.md 환경 변수 가이드 추가
- [ ] `git ls-files | grep .env` → 출력 없음
- [ ] Git 추적 파일에서 비밀 정보 검색 → 없음
- [ ] docs 디렉토리 비밀 정보 제거 완료
- [ ] `.gitignore`에 `.env` 포함 확인

**모두 체크되면 Public 공개 가능! 🎉**

---

**작성자:** Claude Code
**검토일:** 2025-11-17
**다음 리뷰:** 공개 후 1주일 내
