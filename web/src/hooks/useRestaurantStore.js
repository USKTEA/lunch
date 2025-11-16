import useStore from './useStore';
import { restaurantStore } from '../stores/RestaurantStore';

export default function useRestaurantStore() {
  return useStore(restaurantStore);
}
