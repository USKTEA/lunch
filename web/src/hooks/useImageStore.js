import useStore from './useStore';
import { imageStore } from '../stores/ImageStore';

/**
 * ImageStore 구독 Hook
 */
export default function useImageStore() {
  return useStore(imageStore);
}
