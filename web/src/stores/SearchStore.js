import Store from './Store';
import { apiService } from '../service/ApiService';

class SearchStore extends Store {
  constructor() {
    super();
    this.places = [];
    this.isLoading = false;
    this.error = null;

    // 검색 조건
    this.keyword = '';
    this.category = 'all';
    this.sortBy = 'distance';
    this.maxDistance = 500;

    // 검색 중심 위치 (기본값: 서울시청)
    this.centerLocation = {
      lat: 37.5665,
      lon: 126.9780,
    };
  }

  setCenterLocation(lat, lon) {
    this.centerLocation = { lat, lon };
    this.publish();
  }

  setKeyword(keyword) {
    this.keyword = keyword;
    this.publish();
  }

  setCategory(category) {
    this.category = category;
    this.publish();
  }

  setSortBy(sortBy) {
    this.sortBy = sortBy;
    this.publish();
  }

  setMaxDistance(maxDistance) {
    this.maxDistance = maxDistance;
    this.publish();
  }

  getPlaces() {
    return this.places;
  }

  getIsLoading() {
    return this.isLoading;
  }

  getError() {
    return this.error;
  }

  getKeyword() {
    return this.keyword;
  }

  getCategory() {
    return this.category;
  }

  getSortBy() {
    return this.sortBy;
  }

  getMaxDistance() {
    return this.maxDistance;
  }

  getCenterLocation() {
    return this.centerLocation;
  }

  async search() {
    this.isLoading = true;
    this.error = null;
    this.publish();

    try {
      const response = await apiService.searchPlaces({
        centerLat: this.centerLocation.lat,
        centerLon: this.centerLocation.lon,
        keyword: this.keyword || undefined,
        category: this.category,
        sortBy: this.sortBy,
        maxDistance: this.maxDistance,
      });

      this.places = response.places;
      this.isLoading = false;
      this.publish();
    } catch (error) {
      console.error('Search failed:', error);
      this.error = error.message || '검색 중 오류가 발생했습니다.';
      this.isLoading = false;
      this.publish();
    }
  }

  // 필터 변경 시 자동 검색
  async updateAndSearch(updates) {
    if (updates.keyword !== undefined) this.keyword = updates.keyword;
    if (updates.category !== undefined) this.category = updates.category;
    if (updates.sortBy !== undefined) this.sortBy = updates.sortBy;
    if (updates.maxDistance !== undefined) this.maxDistance = updates.maxDistance;

    await this.search();
  }

  clear() {
    this.places = [];
    this.keyword = '';
    this.category = 'all';
    this.sortBy = 'distance';
    this.maxDistance = 500;
    this.error = null;
    this.publish();
  }
}

export const searchStore = new SearchStore();
