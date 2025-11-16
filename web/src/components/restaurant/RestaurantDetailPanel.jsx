import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import useRestaurantStore from '../../hooks/useRestaurantStore';
import useReviewStore from '../../hooks/useReviewStore';
import StarRating from '../ui/StarRating';
import OverviewTab from './OverviewTab';
import ReviewTab from './ReviewTab';

const Panel = styled.div`
  width: 400px;
  height: 100%;
  background: white;
  border-left: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  @media (max-width: 1280px) {
    width: 100%;
    height: 50%;
    border-left: none;
    border-top: 1px solid #e0e0e0;
  }
`;

const Header = styled.div`
  position: relative;
  padding: 24px;
  border-bottom: 1px solid #e0e0e0;
`;

const CloseButton = styled.button`
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 4px;

  &:hover {
    color: #333;
  }
`;

const RestaurantName = styled.h2`
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #202124;
`;

const RatingContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
`;

const Rating = styled.span`
  font-size: 16px;
  font-weight: 500;
  color: #202124;
`;

const Stars = styled.span`
  color: #fbbc04;
  font-size: 14px;
`;

const ReviewCount = styled.span`
  font-size: 14px;
  color: #5f6368;
`;

const CategoryInfo = styled.div`
  font-size: 14px;
  color: #5f6368;
`;

const TabContainer = styled.div`
  display: flex;
  border-bottom: 1px solid #e0e0e0;
`;

const Tab = styled.button`
  flex: 1;
  padding: 16px;
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 500;
  color: ${props => props.$active ? '#1a73e8' : '#5f6368'};
  border-bottom: 2px solid ${props => props.$active ? '#1a73e8' : 'transparent'};
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f8f9fa;
  }
`;

const Content = styled.div`
  flex: 1;
  overflow-y: auto;
  padding: 24px;
`;

/**
 * 식당 상세 정보 패널 (구글 지도 스타일)
 */
function RestaurantDetailPanel() {
  const restaurantStore = useRestaurantStore();
  const reviewStore = useReviewStore();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL에서 탭과 페이징 정보 읽기
  const activeTab = searchParams.get('tab') || 'overview';
  const [pagination, setPagination] = useState({
    pageSize: 10,
    cursor: searchParams.get('cursor') || null,
    hasNext: false
  });

  const selectedRestaurant = restaurantStore.selectedRestaurant;

  // 식당 선택 시 상세 정보 및 리뷰 조회
  useEffect(() => {
    if (selectedRestaurant) {
      restaurantStore.fetchRestaurantDetail(selectedRestaurant.managementNumber);
    }
  }, [selectedRestaurant]);

  // 리뷰 탭 + 페이징 정보 변경 시 리뷰 조회
  useEffect(() => {
    if (selectedRestaurant && activeTab === 'reviews') {
      const loadReviews = async () => {
        await reviewStore.fetchReviews(
          selectedRestaurant.managementNumber,
          pagination.pageSize,
          pagination.cursor
        );
        await reviewStore.fetchReviewRating(selectedRestaurant.managementNumber)

        setPagination(prev => ({
          ...prev,
          hasNext: reviewStore.nextCursor !== null
        }));
      };

      loadReviews();
    }
  }, [selectedRestaurant, activeTab, pagination.cursor]);

  if (!selectedRestaurant) {
    return null;
  }

  const handleClose = () => {
    restaurantStore.clearSelectedRestaurant();
    reviewStore.clearReviews();
    setSearchParams({}); // URL 파라미터 초기화
    setPagination({ pageSize: 10, cursor: null, hasNext: false });
  };

  const handleTabChange = (tab) => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set('tab', tab);
    if (tab !== 'reviews') {
      newParams.delete('cursor'); // 리뷰 탭 아니면 cursor 제거
    }
    setSearchParams(newParams);
  };

  const handleNextPage = (nextCursor) => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set('cursor', nextCursor);
    setSearchParams(newParams);
    setPagination((prev) => ({ ...prev, cursor: nextCursor }));
  };

  const handlePrevPage = (prevCursor) => {
    const newParams = new URLSearchParams(searchParams);
    if (prevCursor) {
      newParams.set('cursor', prevCursor);
    } else {
      newParams.delete('cursor'); // 첫 페이지
    }
    setSearchParams(newParams);
    setPagination(prev => ({ ...prev, cursor: prevCursor }));
  };

  return (
    <Panel>
      <Header>
        <CloseButton onClick={handleClose}>×</CloseButton>
        <RestaurantName>{selectedRestaurant.name}</RestaurantName>

        {selectedRestaurant.category && (
          <CategoryInfo>
            {selectedRestaurant.category}
          </CategoryInfo>
        )}
      </Header>

      <TabContainer>
        <Tab
          $active={activeTab === 'overview'}
          onClick={() => handleTabChange('overview')}
        >
          개요
        </Tab>
        <Tab
          $active={activeTab === 'reviews'}
          onClick={() => handleTabChange('reviews')}
        >
          리뷰
        </Tab>
      </TabContainer>

      <Content>
        {activeTab === 'overview' && <OverviewTab />}
        {activeTab === 'reviews' && (
          <ReviewTab
            pagination={pagination}
            onNextPage={handleNextPage}
            onPrevPage={handlePrevPage}
          />
        )}
      </Content>
    </Panel>
  );
}

export default RestaurantDetailPanel;
