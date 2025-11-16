import useStore from './useStore';
import { placeStore } from '../stores/PlaceStore';

export default function usePlaceStore() {
  return useStore(placeStore);
}
