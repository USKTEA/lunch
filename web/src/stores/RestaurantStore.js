import Store from './Store';
import { apiService } from '../service/ApiService';

/**
 * 레스토랑 도메인 상태 관리
 * UI 상태(검색어, 필터)는 컴포넌트에서 관리
 */
class RestaurantStore extends Store {
  clusters = [];  // H3 셀별 클러스터 (각 클러스터에 restaurants 포함)
  selectedRestaurant = null;  // 선택된 식당 상세 정보
  loading = false;
  error = null;

  constructor() {
    super();
  }

  async fetchRestaurants(boundary, zoomLevel) {
    this.loading = true;
    this.error = null;

    try {
      const data = await apiService.fetchRestaurants(boundary, zoomLevel);

      this.clusters = data.clusters || [];

      this.loading = false;
      this.publish();
    } catch (error) {
      // 401은 ApiService에서 로그인 리다이렉트 처리
      if (error.response?.status === 401) {
        this.loading = false;
        return;
      }
      this.error = error.message;
      this.loading = false;
      this.publish();
    }
  }

  /**
   * 모든 클러스터의 식당을 flat하게 반환
   */
  getAllRestaurants() {
    return this.clusters.flatMap(cluster => cluster.restaurants);
  }

  /**
   * 식당 상세 정보 조회
   */
  async fetchRestaurantBusinessInfo(managementNumber) {
    // managementNumber가 없거나 이미 businessInfo가 있으면 스킵
    if (!managementNumber || this.selectedRestaurant?.businessInfo) {
      return;
    }

    this.loading = true;
    this.error = null;

    try {
      const businessInfo = await apiService.fetchRestaurantBusinessInfo(managementNumber);
      // 기존 selectedRestaurant에 businessInfo를 추가
      this.selectedRestaurant = {
        ...this.selectedRestaurant,
        businessInfo
      };
      this.loading = false;
      this.publish();
    } catch (error) {
      // 401은 ApiService에서 로그인 리다이렉트 처리
      if (error.response?.status === 401) {
        this.loading = false;
        return;
      }
      this.error = error.message;
      this.loading = false;
      this.publish();
    }
  }

  /**
   * 식당 선택 (마커 클릭 시)
   */
  selectRestaurant(restaurant) {
    this.selectedRestaurant = { ...restaurant };
    this.publish();
  }

  /**
   * 선택된 식당 정보 초기화
   */
  clearSelectedRestaurant() {
    this.selectedRestaurant = null;
    this.publish();
  }
}

export const restaurantStore = new RestaurantStore();
