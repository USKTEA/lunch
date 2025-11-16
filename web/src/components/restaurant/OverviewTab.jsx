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

/**
 * 개요 탭 - 식당 기본 정보
 */
function OverviewTab() {
  const restaurantStore = useRestaurantStore();
  const restaurant = restaurantStore.selectedRestaurant;

  if (!restaurant) {
    return <Container>정보를 불러오는 중...</Container>;
  }

  const isOpen = restaurant.currentStatus === '영업 중';

  // 기본 정보만 있는 경우 (상세 정보 API 연동 전)
  const hasDetailInfo = restaurant.businessHours || restaurant.contact || restaurant.address;

  const getDayName = (index) => {
    const days = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
    return days[index];
  };

  const getCurrentDayIndex = () => {
    return new Date().getDay();
  };

  const businessHoursArray = [
    { day: '월요일', hours: restaurant.businessHours?.monday },
    { day: '화요일', hours: restaurant.businessHours?.tuesday },
    { day: '수요일', hours: restaurant.businessHours?.wednesday },
    { day: '목요일', hours: restaurant.businessHours?.thursday },
    { day: '금요일', hours: restaurant.businessHours?.friday },
    { day: '토요일', hours: restaurant.businessHours?.saturday },
    { day: '일요일', hours: restaurant.businessHours?.sunday }
  ];

  const todayIndex = getCurrentDayIndex();

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
      {restaurant.businessHours && (
        <Section>
          <SectionTitle>
            영업 시간
            <StatusBadge $isOpen={isOpen}>{restaurant.currentStatus}</StatusBadge>
          </SectionTitle>
          <BusinessHoursTable>
            {businessHoursArray.map((item, index) => {
              const dayIndex = index === 6 ? 0 : index + 1; // 일요일을 0으로 조정
              const isToday = dayIndex === todayIndex;

              return (
                <DayRow key={item.day}>
                  <DayName $isToday={isToday}>{item.day}</DayName>
                  <DayHours>{item.hours}</DayHours>
                </DayRow>
              );
            })}
          </BusinessHoursTable>
        </Section>
      )}

      {/* 전화번호 */}
      {restaurant.contact && (
        <Section>
          <SectionTitle>전화번호</SectionTitle>
          <InfoRow>
            <Icon>📞</Icon>
            <InfoText>
              <a href={`tel:${restaurant.contact}`} style={{ color: '#1a73e8', textDecoration: 'none' }}>
                {restaurant.contact}
              </a>
            </InfoText>
          </InfoRow>
        </Section>
      )}
    </Container>
  );
}

export default OverviewTab;