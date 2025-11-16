import React from 'react';
import styled from 'styled-components';
import PlaceSearch from './PlaceSearch';
import PlaceList from './PlaceList';

const Panel = styled.aside`
  width: 400px;
  background: #f7f7f7;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;

  @media (max-width: 1280px) {
    width: 100%;
    height: 50%;
  }
`;

/**
 * 맛집 좌측 패널 컴포넌트
 * RestaurantSearch와 RestaurantList를 조합
 */
function PlacePanel() {
  return (
    <Panel>
      <PlaceSearch />
      <PlaceList />
    </Panel>
  );
}

export default PlacePanel;
