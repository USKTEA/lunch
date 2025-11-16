import React from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import useAuthStore from '../hooks/useAuthStore';
import PlacePanel from '../components/restaurant/PlacePanel';
import RestaurantMap from '../components/restaurant/RestaurantMap';
import RestaurantDetailPanel from '../components/restaurant/RestaurantDetailPanel';

const PageContainer = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  overflow: hidden;

  @media (max-width: 1280px) {
    flex-direction: column;
  }
`;

/**
 * 맛집 검색 페이지
 * - 회사 근처 맛집 검색 및 필터링
 * - 지도 기반 맛집 표시
 * - 식당 상세 정보 및 리뷰 (구글 지도 스타일)
 * - 인증되지 않은 경우 LoginPage로 리다이렉트
 */
function MainPage() {
  return (
    <PageContainer>
      <PlacePanel />
      <RestaurantMap />
      <RestaurantDetailPanel />
    </PageContainer>
  );
}

export default MainPage;
