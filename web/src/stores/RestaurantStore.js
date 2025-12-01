import Store from './Store';
import { apiService } from '../service/ApiService';

/**
 * 레스토랑 도메인 상태 관리
 * UI 상태(검색어, 필터)는 컴포넌트에서 관리
 */
class RestaurantStore extends Store {
  restaurants = [];
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

      this.restaurants = data.restaurants;

      this.loading = false;
      this.publish();
    } catch (error) {
      this.error = error.message;
      this.loading = false;
      this.publish();
      throw error;
    }
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
      this.error = error.message;
      this.loading = false;
      this.publish();
      throw error;
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
