import React, { useState } from 'react';
import styled from 'styled-components';

const Container = styled.div`
  display: flex;
  gap: 8px;
  align-items: center;
`;

const Star = styled.button`
  background: none;
  border: none;
  font-size: 32px;
  cursor: pointer;
  padding: 0;
  color: ${props => props.$filled ? '#fbbc04' : '#e0e0e0'};
  transition: color 0.2s, transform 0.1s;

  &:hover {
    transform: scale(1.1);
  }

  &:active {
    transform: scale(0.95);
  }
`;

/**
 * 별점 입력 컴포넌트
 * @param {number} value - 현재 평점 (1-5)
 * @param {function} onChange - 평점 변경 콜백
 */
function StarRatingInput({ value = 0, onChange }) {
  const [hoverRating, setHoverRating] = useState(0);

  const handleClick = (rating) => {
    onChange(rating);
  };

  const displayRating = hoverRating || value;

  return (
    <Container>
      {[1, 2, 3, 4, 5].map(rating => (
        <Star
          key={rating}
          type="button"
          $filled={rating <= displayRating}
          onClick={() => handleClick(rating)}
          onMouseEnter={() => setHoverRating(rating)}
          onMouseLeave={() => setHoverRating(0)}
        >
          {rating <= displayRating ? '★' : '☆'}
        </Star>
      ))}
    </Container>
  );
}

export default StarRatingInput;