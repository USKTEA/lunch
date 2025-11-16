import Store from "./Store";
import { apiService } from "../service/ApiService";
import useStore from "../hooks/useStore";

class UserStore extends Store {
  user = null;

  constructor() {
    super();
  }

  async fetchUser() {
    this.user = await apiService.getUser();

    console.log(this.user)
    this.publish();
  }
}

export const userStore = new UserStore();
