# React Frontend Architecture

## 프로젝트 개요

Lunch 프로젝트의 프론트엔드는 React 18 기반으로 구축되었으며, 커스텀 상태 관리 패턴을 사용합니다.

## 디렉토리 구조

```
web/src/
├── component/          # 재사용 가능한 UI 컴포넌트
├── hooks/              # 커스텀 React Hooks
│   ├── useStore.js    # Store 구독 Hook
│   └── useForceUpdate.js  # 강제 리렌더링 Hook
├── service/           # API 통신 레이어
│   └── ApiService.js  # Axios 기반 HTTP 클라이언트
├── stores/            # 상태 관리 Store 클래스
│   └── Store.js       # Base Store 클래스
├── App.jsx            # 루트 컴포넌트
└── index.js           # 애플리케이션 진입점
```

## 아키텍처 패턴

### 1. 상태 관리: Observer Pattern 기반 Store

#### Store 클래스 (`stores/Store.js`)

커스텀 옵저버 패턴을 구현한 베이스 Store 클래스입니다.

**핵심 개념:**
- 각 Store는 독립적인 상태를 관리
- 상태 변경은 Store의 메서드를 통해서만 가능
- 컴포넌트는 Store를 구독하여 상태 변경 감지

**주요 메서드:**
```javascript
class Store {
  subscribe(listener)   // 리스너 등록
  unsubscribe(listener) // 리스너 해제
  publish()            // 모든 구독자에게 변경 알림 (상태 변경 후 반드시 호출)
}
```

**동작 방식:**
1. Store는 `listeners` Set으로 구독자 관리
2. 상태 변경 시 `publish()` 호출
3. 모든 구독자(컴포넌트)가 리렌더링

#### 사용 예시

```javascript
// 1. Store 정의
class UserStore extends Store {
  user = null;  // 상태는 public 필드로 선언

  setUser(user) {
    this.user = user;
    this.publish(); // 구독자들에게 변경 알림
  }

  clearUser() {
    this.user = null;
    this.publish();
  }
}

export const userStore = new UserStore();

// 2. 컴포넌트에서 사용
function UserProfile() {
  const store = useStore(userStore);

  return <div>{store.user?.name}</div>;  // 상태 직접 접근
}
```

### 2. 컴포넌트-Store 연결: `useStore` Hook

#### `hooks/useStore.js`

Store를 구독하고 상태 변경 시 자동으로 리렌더링하는 커스텀 Hook입니다.

**동작 원리:**
```javascript
function useStore(store) {
  const forceUpdate = useForceUpdate();

  useEffect(() => {
    const unsubscribe = store.subscribe(forceUpdate);
    return unsubscribe; // 컴포넌트 언마운트 시 구독 해제
  }, [store, forceUpdate]);

  return store;
}
```

**흐름:**
1. 컴포넌트 마운트 시 Store에 `forceUpdate` 함수 등록
2. Store의 `publish()` 호출 시 `forceUpdate` 실행
3. 컴포넌트 강제 리렌더링
4. 언마운트 시 자동으로 구독 해제

#### `hooks/useForceUpdate.js`

상태 없이 컴포넌트를 강제로 리렌더링하는 Hook입니다.

```javascript
function useForceUpdate() {
  const [, setCount] = useState(0);
  return () => setCount(count => count + 1);
}
```

### 3. API 통신: ApiService

#### `service/ApiService.js`

Axios 기반 HTTP 클라이언트로 백엔드 API 통신을 담당합니다.

**구조:**
```javascript
class ApiService {
  constructor() {
    this.instance = axios.create({
      baseURL: 'http://localhost:8080'
    });
  }

  async fetchMembers() {
    const response = await this.instance.get('/members');
    return response.data;
  }
}

export const apiService = new ApiService();
```

**특징:**
- 싱글톤 패턴으로 하나의 인스턴스만 생성
- Axios 인스턴스로 공통 설정 관리
- async/await 기반 비동기 처리

## 데이터 흐름

```
┌─────────────┐
│  Component  │ ──① useStore(store) ──┐
└─────────────┘                        │
       ↑                                ↓
       │                         ┌─────────────┐
       │                         │    Store    │
       │                         │  - state    │
       │                         │  - methods  │
       ⑤ 리렌더링               └─────────────┘
       │                                ↓
       │                         ② 메서드 호출
┌─────────────┐                  (setState 등)
│ forceUpdate │ ←─── ④ publish() ─┘
└─────────────┘
       ↑
       │
       └──────── ③ ApiService ─────┐
                (필요 시)            │
                                    ↓
                             ┌──────────────┐
                             │  Backend API │
                             └──────────────┘
```

**흐름 설명:**
1. 컴포넌트가 `useStore(store)`로 Store 구독
2. 사용자 이벤트 → Store 메서드 호출 (예: `store.setUser()`)
3. 필요 시 ApiService로 백엔드 데이터 fetch
4. Store 상태 변경 후 `publish()` 호출
5. 구독 중인 모든 컴포넌트 자동 리렌더링

## 예제: 완전한 기능 구현

### 1. Store 구현

```javascript
// stores/RestaurantStore.js
import Store from './Store';
import { apiService } from '../service/ApiService';

class RestaurantStore extends Store {
  restaurants = [];  // 상태는 public 필드로 선언
  loading = false;
  error = null;

  async fetchRestaurants(lat, lng, radius) {
    this.loading = true;
    this.error = null;
    this.publish();

    try {
      const data = await apiService.fetchNearbyRestaurants(lat, lng, radius);
      this.restaurants = data;
      this.loading = false;
      this.publish();
    } catch (error) {
      this.error = error.message;
      this.loading = false;
      this.publish();
    }
  }

  clearRestaurants() {
    this.restaurants = [];
    this.publish();
  }

  clearError() {
    this.error = null;
    this.publish();
  }
}

export const restaurantStore = new RestaurantStore();
```

### 2. ApiService 확장

```javascript
// service/ApiService.js
class ApiService {
  async fetchNearbyRestaurants(lat, lng, radius) {
    const response = await this.instance.get('/api/restaurants/nearby', {
      params: { lat, lng, radius }
    });
    return response.data;
  }
}
```

### 3. 컴포넌트에서 사용

```javascript
// component/PlaceList.jsx
import React from 'react';
import useStore from '../hooks/useStore';
import { restaurantStore } from '../stores/RestaurantStore';

function RestaurantList() {
  const store = useStore(restaurantStore);

  const handleSearch = () => {
    store.fetchRestaurants(37.5665, 126.9780, 1000);
  };

  if (store.loading) {  // 상태 직접 접근
    return <div>Loading...</div>;
  }

  if (store.error) {  // 상태 직접 접근
    return <div>Error: {store.error}</div>;
  }

  return (
    <div>
      <button onClick={handleSearch}>주변 식당 검색</button>
      <ul>
        {store.restaurants.map(restaurant => (  // 상태 직접 접근
          <li key={restaurant.id}>{restaurant.name}</li>
        ))}
      </ul>
    </div>
  );
}
```

## 장점

### 1. 명확한 관심사 분리
- **Store**: 상태 관리 + 비즈니스 로직
- **ApiService**: API 통신
- **Component**: UI 렌더링 + 사용자 인터랙션

### 2. 예측 가능한 상태 업데이트
- 상태 변경은 Store 메서드를 통해서만 가능
- 단방향 데이터 흐름

### 3. 간단한 구독 메커니즘
- 외부 라이브러리 의존성 없음 (Redux, MobX 불필요)
- 자동 구독/해제로 메모리 누수 방지

### 4. 테스트 용이성
- Store는 순수 JavaScript 클래스 (React 의존성 없음)
- ApiService 모킹 가능

## 단점 및 주의사항

### 1. 세밀한 리렌더링 제어 불가
- Store의 어떤 상태가 바뀌든 모든 구독 컴포넌트가 리렌더링
- 해결: 필요 시 Store를 작은 단위로 분리

### 2. Devtools 부재
- Redux DevTools 같은 디버깅 도구 없음
- 해결: 필요 시 `publish()` 메서드에 로깅 추가

### 3. 비동기 처리 보일러플레이트
- 매번 loading/error 상태 관리 필요
- 해결: Base Store에 공통 메서드 추가 검토

## 개선 제안

### 1. Base Store에 비동기 헬퍼 추가

```javascript
class Store {
  async fetchData(apiCall) {
    this.loading = true;
    this.error = null;
    this.publish();

    try {
      const data = await apiCall();
      this.loading = false;
      this.publish();
      return data;
    } catch (error) {
      this.error = error.message;
      this.loading = false;
      this.publish();
      throw error;
    }
  }
}
```

### 2. Selector 패턴 도입 (선택적)

```javascript
// 특정 속성만 구독 (최적화가 필요한 경우에만 사용)
function useStoreSelector(store, selector) {
  const [value, setValue] = useState(() => selector(store));

  useEffect(() => {
    const listener = () => {
      const newValue = selector(store);
      if (newValue !== value) {
        setValue(newValue);
      }
    };
    return store.subscribe(listener);
  }, [store, selector, value]);

  return value;
}

// 사용 예시
const restaurants = useStoreSelector(
  restaurantStore,
  store => store.restaurants  // 상태 직접 참조
);
```

### 3. ApiService에 인터셉터 추가

```javascript
constructor() {
  this.instance = axios.create({
    baseURL: 'http://localhost:8080'
  });

  // 요청 인터셉터
  this.instance.interceptors.request.use(config => {
    console.log('API 요청:', config.url);
    return config;
  });

  // 응답 인터셉터
  this.instance.interceptors.response.use(
    response => response,
    error => {
      console.error('API 오류:', error);
      return Promise.reject(error);
    }
  );
}
```

## 규칙 (Convention)

### Store 작성 규칙
1. **상속**: 모든 Store는 `Store` 클래스를 상속
2. **상태 선언**: 상태는 **public 필드**로 선언 (class field syntax 사용)
   ```javascript
   class MyStore extends Store {
     data = [];  // ✅ 올바름
     loading = false;
   }
   ```
3. **상태 변경**: 상태 변경 후 반드시 `publish()` 호출
4. **메서드 작성**:
   - ✅ **DO**: 상태를 변경하는 액션 메서드 작성 (예: `setUser()`, `clearData()`, `fetchItems()`)
   - ❌ **DON'T**: 단순 getter 메서드 작성 금지 (예: `getUser()`, `getData()`, `isLoading()`)
     - 이유: 컴포넌트에서 `store.data`로 직접 접근 가능하므로 불필요
     - 예외: 복잡한 계산이 필요한 derived state는 메서드로 작성 가능

### ❌ 잘못된 예시 (Getter 사용)
```javascript
class BadStore extends Store {
  data = [];

  getData() {  // ❌ 불필요한 getter
    return this.data;
  }

  isLoading() {  // ❌ 불필요한 getter
    return this.loading;
  }
}

// 컴포넌트
const store = useStore(badStore);
return <div>{store.getData()}</div>;  // ❌
```

### ✅ 올바른 예시 (직접 접근)
```javascript
class GoodStore extends Store {
  data = [];
  loading = false;

  setData(newData) {  // ✅ 상태 변경 메서드
    this.data = newData;
    this.publish();
  }

  // ✅ derived state는 메서드 허용
  getFilteredData(keyword) {
    return this.data.filter(item => item.name.includes(keyword));
  }
}

// 컴포넌트
const store = useStore(goodStore);
return <div>{store.data.length}</div>;  // ✅ 직접 접근
```

### ApiService 규칙
1. 모든 API 메서드는 async/await 사용
2. 에러는 catch하여 적절히 처리하거나 throw
3. 싱글톤 패턴 유지 (export const apiService)

### 컴포넌트 규칙
1. **Store 구독**: 반드시 `useStore` Hook 사용
2. **상태 읽기**: `store.상태명`으로 직접 접근 (getter 메서드 사용 금지)
3. **상태 변경**: Store의 액션 메서드 호출
4. 컴포넌트는 UI 로직에만 집중

### 요약: Getter를 만들지 마세요!
- `store.user` ✅
- `store.getUser()` ❌
- `store.loading` ✅
- `store.isLoading()` ❌
