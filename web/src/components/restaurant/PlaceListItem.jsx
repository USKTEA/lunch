import React from 'react';
import styled from 'styled-components';
import { formatPrice } from '../../utils/formatUtils';
import StarRating from '../ui/StarRating';

const Item = styled.div`
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 12px;

  &:hover,
  &.hovered {
    border-color: #4ecdc4;
    box-shadow: 0 2px 8px rgba(78, 205, 196, 0.2);
    transform: translateY(-2px);
  }

  &.selected {
    border-color: #ff6b35;
    box-shadow: 0 2px 12px rgba(255, 107, 53, 0.3);
  }

  &:focus {
    outline: 2px solid #4ecdc4;
    outline-offset: 2px;
  }

  &:focus:not(:focus-visible) {
    outline: none;
  }

  @media (max-width: 1400px) {
    flex-direction: column;
  }
`;

const ImageContainer = styled.div`
  position: relative;
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  @media (max-width: 1400px) {
    width: 100%;
    height: 120px;
  }
`;

const ClosedBadge = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 12px;
  font-weight: 600;
`;

const Info = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
`;

const Name = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const Rating = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
`;

const Stars = styled.span`
  display: inline-flex;
  gap: 2px;
`;

const RatingValue = styled.span`
  font-weight: 600;
  color: #333;
`;

const ReviewCount = styled.span`
  color: #666;
  font-size: 13px;
`;

const Meta = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  color: #666;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
`;

const WalkTime = styled.span`
  color: #4ecdc4;
  font-weight: 500;
`;

const Distance = styled.span`
  &::before {
    content: '·';
    margin-right: 8px;
    color: #999;
  }
`;

const Price = styled.span`
  &::before {
    content: '·';
    margin-right: 8px;
    color: #999;
  }
`;

const Tags = styled.div`
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
`;

const Tag = styled.span`
  display: inline-block;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 12px;
  font-size: 11px;
  color: #666;
`;

/**
 * 장소 리스트 아이템 컴포넌트
 */
function PlaceListItem({
  restaurant,
  isSelected = false,
  isHovered = false,
  onClick,
  onMouseEnter,
  onMouseLeave,
}) {
  const {
    name,
    averageRating,
    reviewCount,
    distance,
    walkTime,
    averagePrice,
    mainCategory,
    detailCategory,
  } = restaurant;

  const hasRating = averageRating != null && reviewCount > 0;

  return (
    <Item
      className={`${isSelected ? 'selected' : ''} ${isHovered ? 'hovered' : ''}`}
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      role="button"
      tabIndex={0}
      aria-label={`${name} 상세 정보 보기`}
    >
      <Info>
        <Name>{name}</Name>

        {hasRating ? (
          <Rating>
            <Stars>
              <StarRating rating={averageRating} />
            </Stars>
            <RatingValue>{averageRating.toFixed(1)}</RatingValue>
            <ReviewCount>({reviewCount})</ReviewCount>
          </Rating>
        ) : (
          <Rating>
            <ReviewCount>리뷰 없음</ReviewCount>
          </Rating>
        )}

        <Meta>
          <WalkTime>🚶 도보 {walkTime}분</WalkTime>
          <Distance>{distance}m</Distance>
          {averagePrice > 0 && <Price>💰 평균 {formatPrice(averagePrice)}원</Price>}
        </Meta>

        {(mainCategory || detailCategory) && (
          <Tags>
            {mainCategory && <Tag>#{mainCategory}</Tag>}
            {detailCategory && <Tag>#{detailCategory}</Tag>}
          </Tags>
        )}
      </Info>
    </Item>
  );
}

export default PlaceListItem;
