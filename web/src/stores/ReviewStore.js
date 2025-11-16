import Store from './Store';
import { apiService } from "../service/ApiService";

/**
 * 리뷰 상태 관리
 * 리뷰 목록, 평점 통계 관리
 */
class ReviewStore extends Store {
  reviews = [];
  nextCursor = null; // 다음 페이지 커서
  loading = false;
  error = null;

  // 작성 중인 리뷰 초안
  draftReview = {
    rating: 0,
    content: ''
  };

  rating = null;

  constructor() {
    super();
  }

  /**
   * 특정 식당의 리뷰 목록 조회 (커서 기반 페이징)
   * @param {string} restaurantId - 식당 관리번호
   * @param {number} pageSize - 페이지 크기
   * @param {string|null} cursor - 다음 페이지 커서
   */
  async fetchReviews(restaurantId, pageSize = 10, cursor = null) {
    this.loading = true;
    this.error = null;

    try {
      const response = await apiService.fetchReviews(restaurantId, pageSize, cursor);

      console.log(response);
      // CursorBasedPage 구조: { content: [], meta: { next: string | null } }

      // 커서가 있으면 기존 리뷰에 추가 (더 보기), 없으면 교체 (최초 로드)
      if (cursor) {
        this.reviews = [...this.reviews, ...response.content];
      } else {
        this.reviews = response.content;
      }

      this.nextCursor = response.meta.next;

      this.loading = false;
      this.publish();
    } catch (error) {
      this.error = error.message;
      this.loading = false;
      this.publish();
      throw error;
    }
  }

  /**
   * 리뷰 목록 초기화
   */
  clearReviews() {
    this.reviews = [];
    this.nextCursor = null;
    this.publish();
  }

  /**
   * 작성 중인 리뷰 - 별점 설정
   */
  setDraftRating(rating) {
    this.draftReview.rating = rating;
    this.publish();
  }

  /**
   * 작성 중인 리뷰 - 내용 설정
   */
  setDraftContent(content) {
    this.draftReview.content = content;
    this.publish();
  }

  /**
   * 작성 중인 리뷰 초안 초기화
   */
  resetDraftReview() {
    this.draftReview = {
      rating: 0,
      content: ''
    };
    this.publish();
  }

  /**
   * 유효성 검증 (UI에서 실시간 검증용)
   */
  isDraftValid() {
    return {
      isValid: this.draftReview.rating > 0 &&
               this.draftReview.content.trim().length > 0 &&
               this.draftReview.content.length <= 100,
      isContentOver: this.draftReview.content.length > 100
    };
  }

  /**
   * 리뷰 제출
   */
  async submitReview(restaurantId, imageNames, imageUrls) {
    if (this.draftReview.rating === 0) {
      this.error = '별점을 선택해주세요.';
      this.publish();

      return false;
    }

    if (this.draftReview.content.trim().length === 0) {
      this.error = '리뷰 내용을 입력해주세요.';
      this.publish();

      return false;
    }

    if (this.draftReview.content.length > 100) {
      this.error = '리뷰는 100자 이하로 작성해주세요.';
      this.publish();

      return false;
    }

    try {
      this.loading = true;
      this.error = null;

      console.log('리뷰 제출:', {
        restaurantId,
        rating: this.draftReview.rating,
        content: this.draftReview.content,
        imageNames,
        imageUrls
      });

      await apiService.createReview({
        restaurantManagementNumber: restaurantId,
        rating: this.draftReview.rating,
        content: this.draftReview.content,
        imageNames,
        imageUrls
      });

      // 제출 성공 후 초기화
      this.resetDraftReview();
      this.loading = false;
      this.publish();

      return true;
    } catch (error) {
      this.error = error.message || '리뷰 등록에 실패했습니다.';
      this.loading = false;
      this.publish();
      return false;
    }
  }

  async fetchReviewRating(restaurantId) {
    this.loading = true;
    this.error = null;

    try {
      this.rating = await apiService.fetchRating(restaurantId);
      this.loading = false;
      this.publish();
    } catch (error) {
      this.error = error.message;
      this.loading = false;
      this.publish();
      throw error;
    }
  }

  /**
   * 평점 정보 초기화
   */
  clearRating() {
    this.rating = null;
    this.publish();
  }

  /**
   * 에러 초기화
   */
  clearError() {
    this.error = null;
    this.publish();
  }
}

export const reviewStore = new ReviewStore();
