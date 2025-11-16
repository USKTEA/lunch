# Lunch Web Frontend

React 기반 점심 맛집 찾기 웹 애플리케이션

## 환경 설정

### 1. 환경 변수 설정

`.env.example` 파일을 복사하여 `.env` 파일을 생성하고 실제 값을 입력하세요.

```bash
cp .env.example .env
```

`.env` 파일 내용:
```env
# 네이버 지도 API Client ID
REACT_APP_NAVER_MAP_CLIENT_ID=your_naver_map_client_id_here

# API 서버 URL
REACT_APP_API_BASE_URL=http://localhost:8080
```

### 2. 네이버 지도 API 키 발급

1. [네이버 클라우드 플랫폼](https://www.ncloud.com/)에 접속
2. Console > Services > AI·NAVER API > Maps 선택
3. Application 등록 후 Client ID 발급
4. 발급받은 Client ID를 `.env` 파일에 입력

## 실행 방법

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm start

# 프로덕션 빌드
npm run build
```

## 기술 스택

- React 18
- styled-components
- react-router-dom
- Observer Pattern (Custom Store)
- Naver Maps API

## 프로젝트 구조

```
src/
├── pages/              # 페이지 컴포넌트
├── components/         # 재사용 컴포넌트
│   ├── layout/        # Header, Footer
│   ├── restaurant/    # 맛집 관련 컴포넌트
│   └── ui/            # 공통 UI 컴포넌트
├── stores/            # Observer Pattern Store
├── hooks/             # Custom Hooks
├── utils/             # 유틸리티 함수
└── styles/            # 전역 스타일
```

## 보안

- `.env` 파일은 Git에 커밋되지 않습니다
- API 키 등 민감한 정보는 반드시 환경 변수로 관리하세요
- `.env.example`에는 예시 값만 포함하세요
