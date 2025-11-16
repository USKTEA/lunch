import React, { useState } from 'react';
import styled from 'styled-components';
import {
  CATEGORIES,
  SORT_OPTIONS,
  DISTANCE_OPTIONS,
} from '../../data/places.mock';

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

const CheckboxLabel = styled.label`
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  user-select: none;

  input[type='checkbox'] {
    width: 16px;
    height: 16px;
    cursor: pointer;
    accent-color: #4ecdc4;
  }

  &:hover span {
    color: #4ecdc4;
  }
`;

/**
 * 장소 검색 컴포넌트
 */
function PlaceSearch() {
  const [searchQuery, setSearchQuery] = useState('');
  const [category, setCategory] = useState(CATEGORIES.ALL);
  const [sortBy, setSortBy] = useState(SORT_OPTIONS.DISTANCE);
  const [maxDistance, setMaxDistance] = useState(500);
  const [openOnly, setOpenOnly] = useState(false);

  const handleOpenOnlyToggle = () => {
    setOpenOnly((prev) => !prev);
  };

  return (
    <SearchContainer>
      <SearchBox>
        <SearchIcon>🔍</SearchIcon>
        <SearchInput
          type="text"
          placeholder="맛집 이름이나 태그로 검색..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </SearchBox>

      {/* 카테고리 필터 */}
      <FilterRow className="category-row">
        <CategoryChips>
          <CategoryChip
            $active={category === CATEGORIES.ALL}
            onClick={() => setCategory(CATEGORIES.ALL)}
          >
            전체
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.KOREAN}
            onClick={() => setCategory(CATEGORIES.KOREAN)}
          >
            한식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.CHINESE}
            onClick={() => setCategory(CATEGORIES.CHINESE)}
          >
            중식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.JAPANESE}
            onClick={() => setCategory(CATEGORIES.JAPANESE)}
          >
            일식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.WESTERN}
            onClick={() => setCategory(CATEGORIES.WESTERN)}
          >
            양식
          </CategoryChip>
          <CategoryChip
            $active={category === CATEGORIES.ETC}
            onClick={() => setCategory(CATEGORIES.ETC)}
          >
            기타
          </CategoryChip>
        </CategoryChips>
      </FilterRow>

      {/* 정렬, 거리, 영업중 필터 */}
      <FilterRow className="bottom-row">
        <FilterControls>
          <FilterSelect
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
          >
            <option value={SORT_OPTIONS.DISTANCE}>거리순</option>
            <option value={SORT_OPTIONS.RATING}>평점순</option>
            <option value={SORT_OPTIONS.REVIEW_COUNT}>리뷰순</option>
          </FilterSelect>

          <FilterSelect
            value={maxDistance}
            onChange={(e) => setMaxDistance(Number(e.target.value))}
          >
            <option value={DISTANCE_OPTIONS[0]}>300m 이내</option>
            <option value={DISTANCE_OPTIONS[1]}>500m 이내</option>
            <option value={DISTANCE_OPTIONS[2]}>1000m 이내</option>
          </FilterSelect>

          <CheckboxLabel>
            <input
              type="checkbox"
              checked={openOnly}
              onChange={handleOpenOnlyToggle}
            />
            <span>영업중만</span>
          </CheckboxLabel>
        </FilterControls>
      </FilterRow>
    </SearchContainer>
  );
}

export default PlaceSearch;
