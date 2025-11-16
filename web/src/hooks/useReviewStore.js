import useStore from './useStore';
import { reviewStore } from '../stores/ReviewStore';

export default function useReviewStore() {
  return useStore(reviewStore);
}
