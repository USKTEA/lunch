import axios from "axios";
import { authStore } from '../stores/AuthStore';

// 환경변수에서 API URL 가져오기 (기본값: 프로덕션)
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'https://api.dev-htbeyondcloud.com';

class ApiService {
  static isRedirectingToLogin = false;  // 중복 requireLogin 방지 플래그

  constructor() {
    this.instance = axios.create({
      baseURL: API_BASE_URL,
      withCredentials: true,
      headers: {
        "Accept": "application/json"
      }
    });
    // Request Interceptor - Authorization 헤더 추가
    this.instance.interceptors.request.use(
      (config) => {
        const token = authStore.tokenInfo?.accessToken;
        const tokenType = authStore.tokenInfo?.tokenType || 'Bearer';

        if (token) {
          console.log(token)
          config.headers.Authorization = `${tokenType} ${token}`;
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response Interceptor - 401 처리
    this.instance.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        console.log(error)
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          const errorData = error.response.data;
          console.log('401 Error:', errorData);

          // AuthStore에서 토큰 확인
          const hasAccessToken = authStore.isUserAuthenticated();

          if (hasAccessToken) {
            // 토큰이 있으면 refresh token으로 갱신 시도
            try {
              console.log('Trying to refresh token...');

              const { data } = await axios.post(
                `${API_BASE_URL}/api/auth/tokens`,
                new URLSearchParams({
                  grant_type: 'refresh_token'
                }),
                {
                  headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                  },
                  withCredentials: true
                }
              );

              // AuthStore에 새 토큰 정보 전체 저장
              authStore.setTokenInfo(data);
              ApiService.isRedirectingToLogin = false;  // 리다이렉트 플래그 리셋
              console.log('Token refreshed successfully, expires at:', data.expiresAt);

              // 원래 요청에 새 토큰 추가
              const newToken = authStore.tokenInfo?.accessToken;
              const tokenType = authStore.tokenInfo?.tokenType || 'Bearer';
              originalRequest.headers.Authorization = `${tokenType} ${newToken}`;

              // 재시도
              return this.instance(originalRequest);
            } catch (refreshError) {
              // Refresh token도 만료됨
              console.error('Refresh token expired:', refreshError);

              // 중복 리다이렉트 방지
              if (!ApiService.isRedirectingToLogin) {
                ApiService.isRedirectingToLogin = true;
                authStore.requireLogin(errorData);
              }
              return Promise.reject(refreshError);
            }
          } else {
            // 토큰이 없으면 바로 로그인 화면으로
            console.log('No access token found, redirecting to login...');

            // 중복 리다이렉트 방지
            if (!ApiService.isRedirectingToLogin) {
              ApiService.isRedirectingToLogin = true;
              authStore.requireLogin(errorData);
            }
          }
        }

        return Promise.reject(error);
      }
    );
    this.s3Instance = axios.create()
  }

  async fetchRestaurants(boundary, zoomLevel) {
    try {
      const response = await this.instance.get("/api/restaurants", {
        params: {
          boundary: `${boundary.southWestBoundary.x};${boundary.southWestBoundary.y};${boundary.northEastBoundary.x};${boundary.northEastBoundary.y}`,
          zoomLevel
        }
      });
      return response.data;
    } catch (error) {
      console.error("Error fetching members:", error);
      throw error;
    }
  }

  async fetchRestaurantBusinessInfo(managementNumber) {
    try {
      const response = await this.instance.get(`/api/restaurants/${managementNumber}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching members:", error);
      throw error;
    }
  }

  // Authorization Code를 Access Token으로 교환
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
  };

  async getUser() {
    const response = await this.instance.get(
      "/api/users/me"
    )

    return response.data
  }

  /**
   * Presigned URL 생성 요청
   * @param {Object} request - { context: string, imageMetas: Array<{name: string, imageSize: number, contentType: string}> }
   * @returns {Promise<{preSignedUrls: Array<{name: string, url: string}>}>}
   */
  async createPresignedUrls(request) {
    const response = await this.instance.post(
      "/api/images/presigned-urls",
      request
    );
    return response.data;
  }

  /**
   * S3에 직접 업로드 (Presigned URL 사용)
   * Authorization 헤더 없이 S3로 직접 업로드
   */
  async uploadToS3(presignedUrl, file) {
    await this.s3Instance.put(presignedUrl, file, {
      headers: {
        'Content-Type': file.type,
        'X-Amz-Tagging': 'attached=false'
      },
    });
  }

  async createReview(request) {
    await this.instance.post(
      "/api/reviews",
      request,
    )
  }

  /**
   * 리뷰 목록 조회 (커서 기반 페이징)
   * @param {string} restaurantId - 식당 관리번호
   * @param {number} pageSize - 페이지 크기
   * @param {string|null} cursor - 다음 페이지 커서
   * @returns {Promise<{content: Array, meta: {next: string|null}}>}
   */
  async fetchReviews(restaurantId, pageSize = 10, cursor = null) {
    const params = {
      size: pageSize
    };

    if (cursor) {
      params.cursor = cursor;
    }

    const response = await this.instance.get(
      `/api/reviews/${restaurantId}`,
      { params }
    );

    return response.data;
  }

  async fetchRating(restaurantId) {
    const response = await this.instance.get(
      `/api/reviews/${restaurantId}/rating`);

    return response.data;
  }
}

export const apiService = new ApiService();
