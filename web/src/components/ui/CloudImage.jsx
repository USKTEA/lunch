import React from "react";
import styled from "styled-components";
import { getCloudFrontUrl } from "../../config/constants";

const StyledImage = styled.img`
  width: ${props => props.$displayWidth || '100%'};
  height: ${props => props.$displayHeight || 'auto'};
  object-fit: ${props => props.$objectFit || 'cover'};
  border-radius: ${props => props.$borderRadius || '0'};
`;

/**
 * CloudFront를 통해 이미지를 서빙하는 공통 이미지 컴포넌트
 * width, height는 px 단위 숫자로 전달하면 CloudFront 리사이징 파라미터로 사용됩니다.
 *
 * @param {string} objectKey - S3 객체 키 또는 전체 URL (예: "/image/review/abc.png" 또는 "https://s3.../image/review/abc.png")
 * @param {string} alt - 이미지 대체 텍스트
 * @param {number} width - 이미지 리사이징 너비 (px 단위, CloudFront 쿼리 파라미터로 전달)
 * @param {number} height - 이미지 리사이징 높이 (px 단위, CloudFront 쿼리 파라미터로 전달)
 * @param {string} displayWidth - 화면 표시 너비 (CSS 단위, 기본값: width 또는 '100%')
 * @param {string} displayHeight - 화면 표시 높이 (CSS 단위, 기본값: height 또는 'auto')
 * @param {string} objectFit - object-fit CSS 속성 (기본값: 'cover')
 * @param {string} borderRadius - border-radius CSS 속성 (기본값: '0')
 * @param {string} className - 추가 CSS 클래스
 * @param {function} onClick - 클릭 이벤트 핸들러
 */
function CloudImage({
  objectKey,
  alt = '',
  width,
  height,
  displayWidth,
  displayHeight,
  objectFit,
  borderRadius,
  className,
  onClick,
  ...rest
}) {
  // CloudFront URL 생성 (리사이징 파라미터 포함)
  const imageUrl = getCloudFrontUrl(objectKey, width, height);

  // 화면 표시 크기 계산
  const finalDisplayWidth = displayWidth || (width ? `${width}px` : '100%');
  const finalDisplayHeight = displayHeight || (height ? `${height}px` : 'auto');

  return (
    <StyledImage
      src={imageUrl}
      alt={alt}
      $displayWidth={finalDisplayWidth}
      $displayHeight={finalDisplayHeight}
      $objectFit={objectFit}
      $borderRadius={borderRadius}
      className={className}
      onClick={onClick}
      loading="lazy"
      {...rest}
    />
  );
}

export default CloudImage;