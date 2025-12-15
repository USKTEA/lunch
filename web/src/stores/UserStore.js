import Store from "./Store";
import { apiService } from "../service/ApiService";
import useStore from "../hooks/useStore";

class UserStore extends Store {
  user = null;

  constructor() {
    super();
  }

  async fetchUser() {
    try {
      this.user = await apiService.getUser();
      this.publish();
    } catch (error) {
      // 401은 ApiService에서 로그인 리다이렉트 처리
      if (error.response?.status !== 401) {
        console.error('Failed to fetch user:', error);
      }
    }
  }
}

export const userStore = new UserStore();
