import React from 'react';
import styled from 'styled-components';
import { calculateStarRating } from '../../utils/formatUtils';

const Star = styled.span`
  color: ${(props) => (props.$type === 'full' ? '#FFD700' : '#E0E0E0')};
  font-size: 14px;

  &.half {
    background: linear-gradient(90deg, #ffd700 50%, #e0e0e0 50%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
`;

/**
 * 별점 표시 컴포넌트
 * @param {number} rating - 평점 (0-5)
 */
function StarRating({ rating }) {
  const { fullStars, hasHalfStar, emptyStars } = calculateStarRating(rating);

  return (
    <>
      {Array.from({ length: fullStars }, (_, i) => (
        <Star key={`full-${i}`} $type="full">
          ★
        </Star>
      ))}
      {hasHalfStar && (
        <Star key="half" $type="full" className="half">
          ★
        </Star>
      )}
      {Array.from({ length: emptyStars }, (_, i) => (
        <Star key={`empty-${i}`} $type="empty">
          ☆
        </Star>
      ))}
    </>
  );
}

export default StarRating;
