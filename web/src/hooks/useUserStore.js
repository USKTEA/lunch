import useStore from "./useStore";
import { userStore } from "../stores/UserStore";

export default function useUserStore() {
  return useStore(userStore);
}
