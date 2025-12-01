import React from 'react';
import styled from 'styled-components';
import useRestaurantStore from '../../hooks/useRestaurantStore';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 24px;
`;

const Section = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

const SectionTitle = styled.h3`
  font-size: 14px;
  font-weight: 600;
  color: #202124;
  margin: 0;
`;

const InfoRow = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
`;

const Icon = styled.span`
  font-size: 20px;
  color: #5f6368;
  min-width: 20px;
`;

const InfoText = styled.div`
  font-size: 14px;
  color: #202124;
  line-height: 1.5;
`;

const StatusBadge = styled.span`
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background: ${props => props.$isOpen ? '#e6f4ea' : '#fce8e6'};
  color: ${props => props.$isOpen ? '#137333' : '#c5221f'};
  margin-left: 8px;
`;

const BusinessHoursTable = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

const DayRow = styled.div`
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #202124;
`;

const DayName = styled.span`
  font-weight: ${props => props.$isToday ? '600' : '400'};
  color: ${props => props.$isToday ? '#1a73e8' : '#202124'};
`;

const DayHours = styled.span`
  color: #5f6368;
`;

const MenuList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

const MenuItem = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
`;

const MenuNameContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
`;

const MenuName = styled.span`
  font-size: 14px;
  color: #202124;
`;

const RepresentativeTag = styled.span`
  font-size: 11px;
  padding: 2px 6px;
  background: #e8f0fe;
  color: #1a73e8;
  border-radius: 4px;
  font-weight: 500;
`;

const MenuPrice = styled.span`
  font-size: 14px;
  color: #5f6368;
  font-weight: 500;
`;

const LinkButton = styled.a`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #fee500;
  color: #000;
  text-decoration: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;

  &:hover {
    background: #fdd800;
  }
`;

const BreakTime = styled.span`
  font-size: 12px;
  color: #999;
  margin-left: 8px;
`;

/**
 * 개요 탭 - 식당 기본 정보
 */
function OverviewTab() {
  const restaurantStore = useRestaurantStore();
  const restaurant = restaurantStore.selectedRestaurant;

  if (!restaurant) {
    return <Container>정보를 불러오는 중...</Container>;
  }

  const businessInfo = restaurant.businessInfo;
  const hasDetailInfo = businessInfo?.businessHours || businessInfo?.contact || restaurant.address;

  // 요일 코드를 한글로 변환
  const dayCodeToKorean = {
    SUN: '일요일',
    MON: '월요일',
    TUE: '화요일',
    WED: '수요일',
    THU: '목요일',
    FRI: '금요일',
    SAT: '토요일'
  };

  // 요일 순서 (월요일부터 시작)
  const dayOrder = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

  const getCurrentDayCode = () => {
    const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    return days[new Date().getDay()];
  };

  const todayCode = getCurrentDayCode();

  // 현재 영업 중인지 확인
  const isCurrentlyOpen = () => {
    if (!businessInfo?.businessHours) return false;
    const todayHours = businessInfo.businessHours.find(h => h.day === todayCode);
    return todayHours?.isOpen || false;
  };

  // 시간 포맷팅 (HH:mm:ss -> HH:mm)
  const formatTime = (time) => {
    if (!time) return '';
    return time.substring(0, 5);
  };

  // 영업 시간을 월요일부터 정렬
  const sortedBusinessHours = businessInfo?.businessHours
    ? dayOrder.map(dayCode => {
        const hours = businessInfo.businessHours.find(h => h.day === dayCode);
        return hours || { day: dayCode, isOpen: false };
      })
    : [];

  const isOpen = isCurrentlyOpen();

  return (
    <Container>
      {!hasDetailInfo && (
        <Section>
          <InfoText style={{ color: '#5f6368', textAlign: 'center' }}>
            상세 정보를 불러오는 중입니다...
          </InfoText>
        </Section>
      )}

      {/* 주소 */}
      {restaurant.address && (
        <Section>
          <SectionTitle>주소</SectionTitle>
          <InfoRow>
            <Icon>📍</Icon>
            <InfoText>{restaurant.address}</InfoText>
          </InfoRow>
        </Section>
      )}

      {/* 영업 시간 */}
      {sortedBusinessHours.length > 0 && (
        <Section>
          <SectionTitle>
            영업 시간
            <StatusBadge $isOpen={isOpen}>{isOpen ? '영업 중' : '영업 종료'}</StatusBadge>
          </SectionTitle>
          <BusinessHoursTable>
            {sortedBusinessHours.map((item) => {
              const isToday = item.day === todayCode;
              const hoursText = item.openAt && item.closeAt
                ? `${formatTime(item.openAt)} - ${formatTime(item.closeAt)}`
                : '휴무';
              const hasBreakTime = item.breakTimeStartAt && item.breakTimeEndAt;

              return (
                <DayRow key={item.day}>
                  <DayName $isToday={isToday}>{dayCodeToKorean[item.day]}</DayName>
                  <DayHours>
                    {hoursText}
                    {hasBreakTime && (
                      <BreakTime>
                        (브레이크 {formatTime(item.breakTimeStartAt)}-{formatTime(item.breakTimeEndAt)})
                      </BreakTime>
                    )}
                  </DayHours>
                </DayRow>
              );
            })}
          </BusinessHoursTable>
        </Section>
      )}

      {/* 전화번호 */}
      {businessInfo?.contact && (
        <Section>
          <SectionTitle>전화번호</SectionTitle>
          <InfoRow>
            <Icon>📞</Icon>
            <InfoText>
              <a href={`tel:${businessInfo.contact}`} style={{ color: '#1a73e8', textDecoration: 'none' }}>
                {businessInfo.contact}
              </a>
            </InfoText>
          </InfoRow>
        </Section>
      )}

      {/* 메뉴 */}
      {businessInfo?.menus && businessInfo.menus.length > 0 && (
        <Section>
          <SectionTitle>메뉴</SectionTitle>
          <MenuList>
            {businessInfo.menus.map((menu, index) => (
              <MenuItem key={index}>
                <MenuNameContainer>
                  <MenuName>{menu.name}</MenuName>
                  {menu.isRepresentative && <RepresentativeTag>대표</RepresentativeTag>}
                </MenuNameContainer>
                {menu.price && <MenuPrice>{menu.price.toLocaleString()}원</MenuPrice>}
              </MenuItem>
            ))}
          </MenuList>
        </Section>
      )}

      {/* 카카오맵 링크 */}
      {businessInfo?.link && (
        <Section>
          <SectionTitle>더보기</SectionTitle>
          <LinkButton href={businessInfo.link} target="_blank" rel="noopener noreferrer">
            카카오맵에서 보기
          </LinkButton>
        </Section>
      )}
    </Container>
  );
}

export default OverviewTab;