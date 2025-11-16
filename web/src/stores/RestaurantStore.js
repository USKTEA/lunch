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
   * TODO: 백엔드 API 연동
   */
  async fetchRestaurantDetail(managementNumber) {
    this.loading = true;
    this.error = null;

    try {
      // TODO: 백엔드 API 구현 후 주석 해제
      // const data = await apiService.fetchRestaurantDetail(managementNumber);
      // this.selectedRestaurant = { ...this.selectedRestaurant, ...data };

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
   * 선택된 식당 설정 (마커 클릭 시 호출)
   */
  setSelectedRestaurant(restaurant) {
    this.selectedRestaurant = restaurant;
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
