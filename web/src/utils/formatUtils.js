/**
 * 포맷팅 유틸리티 함수
 */

/**
 * 가격을 한국 로케일 포맷으로 변환
 * @param {number} price - 가격
 * @returns {string} 포맷된 가격 문자열
 */
export const formatPrice = (price) => {
  return price.toLocaleString('ko-KR');
};

/**
 * 평점을 별 데이터로 변환
 * @param {number} rating - 평점 (0-5)
 * @returns {Object} { fullStars, hasHalfStar, emptyStars }
 */
export const calculateStarRating = (rating) => {
  const fullStars = Math.floor(rating);
  const hasHalfStar = rating % 1 >= 0.5;
  const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

  return {
    fullStars,
    hasHalfStar,
    emptyStars,
  };
};
