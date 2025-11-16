import useStore from './useStore';
import { mapStore } from '../stores/MapStore';

export default function useMapStore() {
  return useStore(mapStore);
}
