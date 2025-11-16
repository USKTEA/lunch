import useStore from "./useStore";
import { authStore } from "../stores/AuthStore";

export default function useAuthStore() {
  return useStore(authStore);
}
