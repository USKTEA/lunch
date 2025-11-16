import React, { useState, useEffect, useRef, useCallback } from "react";
import styled from "styled-components";
import useRestaurantStore from "../../hooks/useRestaurantStore";
import useReviewStore from "../../hooks/useReviewStore";
import StarRating from "../ui/StarRating";
import ReviewItem from "./ReviewItem";
import ReviewWriteModal from "./ReviewWriteModal";

const Container = styled.div`
    display: flex;
    flex-direction: column;
    gap: 24px;
`;

const RatingSummary = styled.div`
    padding: 20px;
    background: #f8f9fa;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
`;

const AverageRating = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
`;

const RatingNumber = styled.div`
    font-size: 48px;
    font-weight: 600;
    color: #202124;
`;

const Stars = styled.div`
    font-size: 20px;
    color: #fbbc04;
`;

const TotalReviews = styled.div`
    font-size: 14px;
    color: #5f6368;
`;

const RatingDistribution = styled.div`
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 8px;
`;

const RatingRow = styled.div`
    display: flex;
    align-items: center;
    gap: 8px;
`;

const StarLabel = styled.span`
    font-size: 12px;
    color: #fbbc04;
    min-width: 40px;
`;

const ProgressBarContainer = styled.div`
    flex: 1;
    height: 8px;
    background: #e0e0e0;
    border-radius: 4px;
    overflow: hidden;
`;

const ProgressBar = styled.div`
    height: 100%;
    background: #fbbc04;
    width: ${props => props.$width}%;
    transition: width 0.3s ease;
`;

const Count = styled.span`
    font-size: 12px;
    color: #5f6368;
    min-width: 30px;
    text-align: right;
`;

const WriteReviewButton = styled.button`
    width: 100%;
    padding: 12px;
    background: #1a73e8;
    color: white;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
        background: #1765cc;
    }

    &:active {
        background: #1557b0;
    }
`;

const ReviewList = styled.div`
    display: flex;
    flex-direction: column;
    gap: 20px;
    padding-top: 8px;
`;

const NoReviews = styled.div`
    text-align: center;
    padding: 40px 20px;
    color: #5f6368;
    font-size: 14px;
`;

const LoadingIndicator = styled.div`
    text-align: center;
    padding: 20px;
    color: #5f6368;
    font-size: 14px;
`;

/**
 * 리뷰 탭 - 평점 요약 + 리뷰 목록 (무한 스크롤)
 */
function ReviewTab() {
  const restaurantStore = useRestaurantStore();
  const reviewStore = useReviewStore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const observerTarget = useRef(null);

  const restaurant = restaurantStore.selectedRestaurant;
  const reviews = reviewStore.reviews;
  const rating = reviewStore.rating;
  const nextCursor = reviewStore.nextCursor;
  const loading = reviewStore.loading;

  // 컴포넌트 마운트 시 리뷰 및 평점 조회
  useEffect(() => {
    if (restaurant) {
      reviewStore.fetchReviews(restaurant.managementNumber, 10);
      reviewStore.fetchReviewRating(restaurant.managementNumber);
    }

    return () => {
      reviewStore.clearReviews();
      reviewStore.clearRating();
    };
  }, [restaurant?.managementNumber]);

  // Intersection Observer로 무한 스크롤 구현
  const handleObserver = useCallback((entries) => {
    const target = entries[0];
    if (target.isIntersecting && nextCursor && !loading) {
      reviewStore.fetchReviews(restaurant.managementNumber, 10, nextCursor);
    }
  }, [nextCursor, loading, restaurant?.managementNumber]);

  useEffect(() => {
    const observer = new IntersectionObserver(handleObserver, {
      threshold: 0.1,
      rootMargin: '100px'  // 뷰포트 하단 100px 전에 미리 로드
    });

    const currentTarget = observerTarget.current;
    if (currentTarget) {
      observer.observe(currentTarget);
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget);
      }
    };
  }, [handleObserver]);

  if (!restaurant) {
    return <Container>정보를 불러오는 중...</Container>;
  }

  const calculatePercentage = (count) => {
    if (!rating || rating.totalReviews === 0) return 0;
    return (count / rating.totalReviews) * 100;
  };

  const handleWriteReview = () => {
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
  };

  const handleReviewSuccess = () => {
    // 리뷰 작성 성공 시 리뷰 목록 및 평점 다시 조회
    reviewStore.clearReviews();
    reviewStore.fetchReviews(restaurant.managementNumber, 10);
    reviewStore.fetchReviewRating(restaurant.managementNumber);
  };

  return (
    <Container>
      {rating ? (
        <RatingSummary>
          <AverageRating>
            <RatingNumber>{rating.average.toFixed(1)}</RatingNumber>
            <Stars>
              <StarRating rating={rating.average} />
            </Stars>
            <TotalReviews>리뷰 {rating.totalReviews}개</TotalReviews>
          </AverageRating>

          <RatingDistribution>
            <RatingRow>
              <StarLabel>{"★".repeat(5)}</StarLabel>
              <ProgressBarContainer>
                <ProgressBar $width={calculatePercentage(rating.rating5Count)} />
              </ProgressBarContainer>
              <Count>{rating.rating5Count}</Count>
            </RatingRow>
            <RatingRow>
              <StarLabel>{"★".repeat(4)}{"☆"}</StarLabel>
              <ProgressBarContainer>
                <ProgressBar $width={calculatePercentage(rating.rating4Count)} />
              </ProgressBarContainer>
              <Count>{rating.rating4Count}</Count>
            </RatingRow>
            <RatingRow>
              <StarLabel>{"★".repeat(3)}{"☆".repeat(2)}</StarLabel>
              <ProgressBarContainer>
                <ProgressBar $width={calculatePercentage(rating.rating3Count)} />
              </ProgressBarContainer>
              <Count>{rating.rating3Count}</Count>
            </RatingRow>
            <RatingRow>
              <StarLabel>{"★".repeat(2)}{"☆".repeat(3)}</StarLabel>
              <ProgressBarContainer>
                <ProgressBar $width={calculatePercentage(rating.rating2Count)} />
              </ProgressBarContainer>
              <Count>{rating.rating2Count}</Count>
            </RatingRow>
            <RatingRow>
              <StarLabel>{"★"}{"☆".repeat(4)}</StarLabel>
              <ProgressBarContainer>
                <ProgressBar $width={calculatePercentage(rating.rating1Count)} />
              </ProgressBarContainer>
              <Count>{rating.rating1Count}</Count>
            </RatingRow>
          </RatingDistribution>

          <WriteReviewButton onClick={handleWriteReview}>
            리뷰 작성
          </WriteReviewButton>
        </RatingSummary>
      ) : (
        <RatingSummary>
          <WriteReviewButton onClick={handleWriteReview}>
            첫 번째 리뷰를 작성해보세요!
          </WriteReviewButton>
        </RatingSummary>
      )}
      {
        reviews.length === 0 ? (
          <NoReviews>아직 리뷰가 없습니다. 첫 번째 리뷰를 작성해보세요!</NoReviews>
        ) : (
          <ReviewList>
            {reviews.map((review, index) => (
              <ReviewItem key={`${review.reviewerId}-${review.createdAt}-${index}`} review={review} />
            ))}
            {/* Intersection Observer 타겟 */}
            {nextCursor && (
              <div ref={observerTarget} style={{ height: '20px' }}>
                {loading && <LoadingIndicator>로딩 중...</LoadingIndicator>}
              </div>
            )}
          </ReviewList>
        )
      }
      <ReviewWriteModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onSuccess={handleReviewSuccess}
      />
    </Container>
  );
}

export default ReviewTab;
