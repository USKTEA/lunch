import React, { useEffect } from "react";
import styled from "styled-components";
import useMapStore from "../../hooks/useMapStore";
import useRestaurantStore from "../../hooks/useRestaurantStore";
import useAuthStore from "../../hooks/useAuthStore";

const MapContainer = styled.div`
    flex: 1;
    position: relative;
    overflow: hidden;
    background: #e8f4f3;
`;

const Map = styled.div`
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
`;

/**
 * 지도 컴포넌트
 * 네이버 지도 API를 사용하여 위치 표시
 */
function RestaurantMap() {
  const mapStore = useMapStore();
  const restaurantStore = useRestaurantStore();

  const fetchRestaurantsAndAddMarkers = async () => {
    await restaurantStore.fetchRestaurants(
      mapStore.currentBoundary(),
      mapStore.currentZoomLevel()
    );

    // 마커 클릭 시 선택된 식당 업데이트
    mapStore.addRestaurantMarkers(
      restaurantStore.restaurants,
      (restaurant) => {
        restaurantStore.setSelectedRestaurant(restaurant);
      }
    );
  };

  const handleSetLocation = (location) => {
    mapStore.initializeMap("map");
    mapStore.map.addListener("idle", fetchRestaurantsAndAddMarkers);
    fetchRestaurantsAndAddMarkers();
  };

  const handleLocationError = (error) => {
    console.error("Geolocation error:", error);

    mapStore.initializeMap("map");
    mapStore.map.addListener("idle", fetchRestaurantsAndAddMarkers);
    fetchRestaurantsAndAddMarkers();
  };

  const geolocationOptions = {
    enableHighAccuracy: true,
    timeout: 5000,
    maximumAge: 0
  };

  useEffect(() => {
    if (!window.naver || !window.navigator.geolocation) {
      return;
    }

    window.navigator.geolocation.getCurrentPosition(
      handleSetLocation,
      handleLocationError,
      geolocationOptions
    );
  }, []);

  return (
    <MapContainer>
      <Map id="map" />
    </MapContainer>
  );
}

export default RestaurantMap;
