import React, { useEffect, useCallback } from 'react';
import styled from 'styled-components';
import useStore from '../../hooks/useStore';
import { searchStore } from '../../stores/SearchStore';
import { mapStore } from '../../stores/MapStore';

const CATEGORIES = {
  ALL: 'all',
  KOREAN: 'KOREAN',
  CHINESE: 'CHINESE',
  JAPANESE: 'JAPANESE',
  WESTERN: 'WESTERN',
  ETC: 'etc',
};

const SORT_OPTIONS = {
  DISTANCE: 'distance',
  RATING: 'rating',
  REVIEW_COUNT: 'reviewCount',
};

const DISTANCE_OPTIONS = [300, 500, 1000];

const SearchContainer = styled.div`
  width: 400px;
  background: #ffffff;
  border-bottom: 1px solid #e0e0e0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex-shrink: 0;
`;

const SearchBox = styled.div`
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  background: #f7f7f7;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 10px 12px;
  transition: border 0.2s;

  &:focus-within {
    border-color: #4ecdc4;
    background: #ffffff;
  }
`;

const SearchIcon = styled.span`
  margin-right: 8px;
  font-size: 16px;
  color: #999;
`;

const SearchInput = styled.input`
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: #333;

  &::placeholder {
    color: #999;
  }
`;

const FilterRow = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;

  &.category-row {
    overflow-x: auto;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  &.bottom-row {
    justify-content: flex-start;
  }
`;

const CategoryChips = styled.div`
  display: flex;
  gap: 8px;
`;

const CategoryChip = styled.button`
  padding: 8px 16px;
  background: ${(props) => (props.$active ? '#4ECDC4' : '#F0F0F0')};
  color: ${(props) => (props.$active ? 'white' : '#666')};
  border: 1px solid ${(props) => (props.$active ? '#4ECDC4' : '#E0E0E0')};
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    border-color: #4ecdc4;
    background: ${(props) => (props.$active ? '#4ECDC4' : '#E8F9F8')};
    color: ${(props) => (props.$active ? 'white' : '#4ECDC4')};
  }
`;

const FilterControls = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
`;

const FilterSelect = styled.select`
  padding: 8px 12px;
  background: #f7f7f7;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: border 0.2s;

  &:hover,
  &:focus {
    border-color: #4ecdc4;
    outline: none;
  }
`;

/**
 * 장소 검색 컴포넌트
 */
function PlaceSearch() {
  useStore(searchStore);
  useStore(mapStore);

  const keyword = searchStore.getKeyword();
  const category = searchStore.getCategory();
  const sortBy = searchStore.getSortBy();
  const maxDistance = searchStore.getMaxDistance();

  // 지도 기본 위치를 검색 중심 위치로 설정 및 초기 검색
  useEffect(() => {
    const position = mapStore.defaultPosition;
    if (position) {
      searchStore.setCenterLocation(position.y, position.x);
    }
    searchStore.search();
  }, []);

  // 디바운스된 검색 (키워드 변경 시)
  useEffect(() => {
    const timer = setTimeout(() => {
      searchStore.search();
    }, 300);
    return () => clearTimeout(timer);
  }, [keyword]);

  const handleKeywordChange = useCallback((e) => {
    searchStore.setKeyword(e.target.value);
  }, []);

  const handleCategoryChange = useCallback((newCategory) => {
    searchStore.setCategory(newCategory);
    searchStore.search();
  }, []);

  const handleSortByChange = useCallback((e) => {
    searchStore.setSortBy(e.target.value);
    searchStore.search();
  }, []);

  const handleMaxDistanceChange = useCallback((e) => {
    searchStore.setMaxDistance(Number(e.target.value));
    searchStore.search();
  }, []);

  return (
    <SearchContainer>
      <SearchBox>
        <SearchIcon>🔍</SearchIcon>
        <SearchInput
          type="text"
          placeholder="맛집 이름으로 검색..."
          value={keyword}
          onChange={handleKeywordChange}
        />
      </SearchBox>

      {/* 카테고리 필터 */}
      <FilterRow className="category-row">
        <CategoryChips>
          <CategoryChip
            $active={category === CATEGORIES.ALL}
            onClick={() => handleCategoryChange(CATEGORIES.ALL)}
          >
            전체
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.KOREAN}
            onClick={() => handleCategoryChange(CATEGORIES.KOREAN)}
          >
            한식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.CHINESE}
            onClick={() => handleCategoryChange(CATEGORIES.CHINESE)}
          >
            중식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.JAPANESE}
            onClick={() => handleCategoryChange(CATEGORIES.JAPANESE)}
          >
            일식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.WESTERN}
            onClick={() => handleCategoryChange(CATEGORIES.WESTERN)}
          >
            양식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.ETC}
            onClick={() => handleCategoryChange(CATEGORIES.ETC)}
          >
            기타
          </CategoryChip>
        </CategoryChips>
      </FilterRow>

      {/* 정렬, 거리 필터 */}
      <FilterRow className="bottom-row">
        <FilterControls>
          <FilterSelect value={sortBy} onChange={handleSortByChange}>
            <option value={SORT_OPTIONS.DISTANCE}>거리순</option>
            <option value={SORT_OPTIONS.RATING}>평점순</option>
            <option value={SORT_OPTIONS.REVIEW_COUNT}>리뷰순</option>
          </FilterSelect>

          <FilterSelect value={maxDistance} onChange={handleMaxDistanceChange}>
            <option value={DISTANCE_OPTIONS[0]}>300m 이내</option>
            <option value={DISTANCE_OPTIONS[1]}>500m 이내</option>
            <option value={DISTANCE_OPTIONS[2]}>1000m 이내</option>
          </FilterSelect>
        </FilterControls>
      </FilterRow>
    </SearchContainer>
  );
}

export default PlaceSearch;
