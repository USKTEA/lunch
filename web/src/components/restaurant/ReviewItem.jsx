import React from 'react';
import styled from 'styled-components';
import StarRating from '../ui/StarRating';
import CloudImage from '../ui/CloudImage';

const Container = styled.div`
  padding-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
`;

const Header = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
`;

const Avatar = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: ${props => props.$color || '#1a73e8'};
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
`;

const UserInfo = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

const UserName = styled.div`
  font-size: 14px;
  font-weight: 500;
  color: #202124;
`;

const RatingAndDate = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

const DateText = styled.span`
  font-size: 12px;
  color: #5f6368;
`;

const Content = styled.div`
  font-size: 14px;
  line-height: 1.6;
  color: #202124;
  margin-bottom: 12px;
`;

const Images = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
`;

const ImageWrapper = styled.div`
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.05);
  }
`;

const Footer = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
`;

const LikeButton = styled.button`
  background: none;
  border: none;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
  color: #5f6368;
  font-size: 12px;

  &:hover {
    background: #f1f3f4;
  }
`;

/**
 * 개별 리뷰 아이템
 */
function ReviewItem({ review }) {
  const formatDate = (dateString) => {
    // ISO-8601 문자열을 Date로 파싱
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
  };

  const getAvatarColor = (userId) => {
    const colors = ['#1a73e8', '#ea4335', '#fbbc04', '#34a853', '#9334e9', '#e91e63'];
    return colors[userId % colors.length];
  };

  const getInitial = (name) => {
    if (!name || name.length === 0) return '?';
    return name.charAt(0).toUpperCase();
  };

  const handleImageClick = (imageUrl) => {
    // TODO: 이미지 모달/갤러리 열기
    console.log('Open image:', imageUrl);
  };

  return (
    <Container>
      <Header>
        <Avatar $color={getAvatarColor(review.reviewerId)}>
          {getInitial(review.reviewerNickname)}
        </Avatar>
        <UserInfo>
          <UserName>{review.reviewerNickname || '익명'}</UserName>
          <RatingAndDate>
            <StarRating rating={review.rating} />
            <DateText>{formatDate(review.createdAt)}</DateText>
          </RatingAndDate>
        </UserInfo>
      </Header>

      <Content>{review.content}</Content>

      {review.imageUrls && review.imageUrls.length > 0 && (
        <Images>
          {review.imageUrls.map((imageUrl, index) => (
            <ImageWrapper key={index} onClick={() => handleImageClick(imageUrl)}>
              <CloudImage
                objectKey={imageUrl}
                alt={`리뷰 이미지 ${index + 1}`}
                width={240}
                height={240}
                displayWidth="100%"
                displayHeight="120px"
                objectFit="cover"
                borderRadius="8px"
              />
            </ImageWrapper>
          ))}
        </Images>
      )}
    </Container>
  );
}

export default ReviewItem;
