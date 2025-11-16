/**
 * 애플리케이션 전역 상수
 */

// CloudFront 도메인
export const CLOUDFRONT_DOMAIN = 'static.dev-htbeyondcloud.com';

/**
 * CloudFront URL 생성 헬퍼 (이미지 리사이징 지원)
 * @param {string} objectKey - S3 객체 키 또는 전체 URL
 * @param {number} width - 이미지 너비 (px 단위, 선택)
 * @param {number} height - 이미지 높이 (px 단위, 선택)
 * @returns {string} CloudFront URL with resize parameters
 */
export const getCloudFrontUrl = (objectKey, width = null, height = null) => {
  if (!objectKey) return '';

  // objectKey에서 /image/** 부분 추출
  // 예: "https://htbeyond-lunch.s3.ap-northeast-2.amazonaws.com/image/review/abc.png"
  // -> "/image/review/abc.png"
  const keyMatch = objectKey.match(/\/image\/.+$/);
  if (!keyMatch) {
    console.warn('Invalid objectKey format:', objectKey);
    return objectKey; // 원본 반환
  }

  const path = keyMatch[0];
  let url = `https://${CLOUDFRONT_DOMAIN}${path}`;

  // 리사이징 파라미터 추가
  const params = new URLSearchParams();
  if (width) params.append('width', width);
  if (height) params.append('height', height);

  const queryString = params.toString();
  if (queryString) {
    url += `?${queryString}`;
  }

  return url;
};
