import React, { useState } from 'react';
import styled from 'styled-components';
import PlaceListItem from './PlaceListItem';
import useStore from '../../hooks/useStore';
import { searchStore } from '../../stores/SearchStore';

const ListContainer = styled.div`
  width: 400px;
  height: calc(100vh - 130px);
  background: #f7f7f7;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
`;

const ListHeader = styled.div`
  padding: 16px;
  background: #ffffff;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
`;

const ResultCount = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: #333;
`;

const ListItems = styled.div`
  flex: 1;
  overflow-y: auto;
  padding: 16px;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: #f0f0f0;
  }

  &::-webkit-scrollbar-thumb {
    background: #c0c0c0;
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #a0a0a0;
  }
`;

const LoadingState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #666;
`;

const Spinner = styled.div`
  width: 40px;
  height: 40px;
  border: 4px solid #e0e0e0;
  border-top-color: #4ecdc4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
`;

const ErrorState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  padding: 24px;
  text-align: center;

  p {
    font-size: 16px;
    color: #f5222d;
    margin: 0;
  }

  button {
    padding: 10px 20px;
    background: #ff6b35;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #e55a2a;
    }
  }
`;

const EmptyState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  padding: 24px;
  text-align: center;

  p {
    font-size: 16px;
    color: #666;
    margin: 0;
  }

  button {
    padding: 10px 20px;
    background: #4ecdc4;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #3fb8b0;
    }
  }
`;

/**
 * 맛집 리스트 컴포넌트
 * searchStore.places를 표시
 */
function PlaceList() {
  useStore(searchStore);
  const [selectedPlaceId, setSelectedPlaceId] = useState(null);
  const [hoveredPlaceId, setHoveredPlaceId] = useState(null);

  const places = searchStore.getPlaces();
  const isLoading = searchStore.getIsLoading();
  const error = searchStore.getError();

  const handlePlaceClick = (id) => {
    setSelectedPlaceId((prev) => (prev === id ? null : id));
  };

  if (isLoading) {
    return (
      <ListContainer>
        <LoadingState>
          <Spinner />
          <p>검색 중...</p>
        </LoadingState>
      </ListContainer>
    );
  }

  if (error) {
    return (
      <ListContainer>
        <ErrorState>
          <p>{error}</p>
          <button onClick={() => searchStore.search()}>다시 시도</button>
        </ErrorState>
      </ListContainer>
    );
  }

  if (places.length === 0) {
    return (
      <ListContainer>
        <EmptyState>
          <p>검색 결과가 없습니다.</p>
        </EmptyState>
      </ListContainer>
    );
  }

  return (
    <ListContainer>
      <ListHeader>
        <ResultCount>총 {places.length}개의 장소</ResultCount>
      </ListHeader>
      <ListItems>
        {places.map((place) => (
          <PlaceListItem
            key={place.managementNumber}
            restaurant={place}
            isSelected={selectedPlaceId === place.managementNumber}
            isHovered={hoveredPlaceId === place.managementNumber}
            onClick={() => handlePlaceClick(place.managementNumber)}
            onMouseEnter={() => setHoveredPlaceId(place.managementNumber)}
            onMouseLeave={() => setHoveredPlaceId(null)}
          />
        ))}
      </ListItems>
    </ListContainer>
  );
}

export default PlaceList;
