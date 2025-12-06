import Store from './Store';
import { apiService } from '../service/ApiService';

class AuthStore extends Store {
  tokenInfo = null;  // { accessToken, issuedAt, tokenType, expiresIn, expiresAt }
  providers = [];
  redirectAfterLogin = null;
  navigate = null;  // React Router의 navigate 함수

  constructor() {
    super();
  }

  startWith(provider, redirectUri) {
    console.log('Starting OAuth2 login with:', provider);

    const state = this.generateUUID();
    sessionStorage.setItem('state', state);
    const apiBaseUrl = process.env.REACT_APP_API_BASE_URL || 'https://api.dev-htbeyondcloud.com';
    window.location.href = `${apiBaseUrl}${provider.authorizationUri}?redirect_uri=${redirectUri}&state=${state}`;
  }

  // UUID v4 생성 (crypto.randomUUID 대체)
  generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  // Authorization Code를 Token으로 교환
  async getToken(code, grantType, redirectUri) {
    try {
      const state = sessionStorage.getItem('state');
      const tokenData = await apiService.getToken(code, grantType, redirectUri, state);

      this.setTokenInfo(tokenData);
      this.clearState();
    } catch (error) {
      console.error('Failed to exchange code for token:', error);
      throw error;
    }
  }

  // state 정리
  clearState() {
    this.state = null;
    sessionStorage.removeItem('oauth_state');
  }

  // React Router의 navigate 함수 주입
  setNavigate(navigateFunc) {
    this.navigate = navigateFunc;
  }

  // localStorage에서 토큰 복원
  loadToken() {
    const tokenInfoJson = localStorage.getItem('token_info');
    if (tokenInfoJson) {
      try {
        this.tokenInfo = JSON.parse(tokenInfoJson);
      } catch (error) {
        console.error('Failed to parse token info:', error);
        this.clearToken();
      }
    }
  }

  // 토큰 정보 저장
  setTokenInfo(tokenData) {
    this.tokenInfo = {
      accessToken: tokenData.accessToken,
      issuedAt: tokenData.issuedAt,
      tokenType: tokenData.tokenType,
      expiresIn: tokenData.expiresIn,
      expiresAt: tokenData.expiresAt,
    };

    localStorage.setItem('token_info', JSON.stringify(this.tokenInfo));
    this.publish();
  }

  // 토큰 만료 여부 확인
  isTokenExpired() {
    if (!this.tokenInfo?.expiresAt) {
      return true;
    }

    // expiresAt은 ISO 8601 문자열 또는 타임스탬프
    const expiresAt = new Date(this.tokenInfo.expiresAt * 1000);
    const now = new Date();

    console.log(expiresAt)
    // 30초 버퍼 (만료 30초 전부터 갱신 가능하도록)
    return expiresAt.getTime() - now.getTime() < 30000;
  }

  // 인증 상태 확인
  isUserAuthenticated() {
    return this.tokenInfo !== null;
  }

  // 토큰 삭제
  clearToken() {
    this.tokenInfo = null;
    localStorage.removeItem('token_info');
    this.publish();
  }

  // Provider 정보 저장
  setProviders(providers) {
    this.providers = providers;
    this.publish();
  }

  // 로그인 후 리다이렉트 경로 저장
  setRedirectAfterLogin(path) {
    this.redirectAfterLogin = path;
    sessionStorage.setItem('redirect_after_login', path);
    this.publish();
  }

  // 로그인 후 리다이렉트 경로 가져오기
  getRedirectAfterLogin() {
    if (!this.redirectAfterLogin) {
      this.redirectAfterLogin = sessionStorage.getItem('redirect_after_login');
    }
    return this.redirectAfterLogin;
  }

  // 로그인 후 리다이렉트 경로 삭제
  clearRedirectAfterLogin() {
    this.redirectAfterLogin = null;
    sessionStorage.removeItem('redirect_after_login');
    this.publish();
  }

  // 로그아웃
  logout() {
    this.tokenInfo = null;
    this.providers = [];
    this.redirectAfterLogin = null;
    this.state = null;
    localStorage.removeItem('token_info');
    sessionStorage.removeItem('redirect_after_login');
    sessionStorage.removeItem('oauth_state');
    this.publish();
  }

  // 로그인 필요 (401 처리)
  requireLogin(errorData) {
    // 1. 기존 토큰 삭제
    this.clearToken();

    // 2. 이전 state 정리
    this.clearState();

    // 3. 현재 경로 저장
    this.setRedirectAfterLogin(window.location.pathname);

    console.log(errorData)
    // 4. Provider 정보 저장
    if (errorData?.providers) {
      this.setProviders(errorData.providers);
    }

    // 5. 로그인 페이지로 이동 (SPA 방식, 메모리 유지)
    if (this.navigate) {
      this.navigate('/web/login');
    } else {
      // navigate가 주입되지 않은 경우 fallback
      window.location.href = '/web/login';
    }
  }
}

export const authStore = new AuthStore();
