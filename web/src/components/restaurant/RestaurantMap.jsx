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
    const zoomLevel = mapStore.currentZoomLevel();

    await restaurantStore.fetchRestaurants(
      mapStore.currentBoundary(),
      zoomLevel
    );

    // zoomLevel 14~16: 클러스터 폴리곤 표시
    if (zoomLevel <= 16) {
      mapStore.clearRestaurantMarkers();
      mapStore.addClusterPolygons(
        restaurantStore.clusters,
        (cluster) => {
          // 클러스터 클릭 시 해당 셀 중심으로 줌인
          const center = new naver.maps.LatLng(cluster.center.y, cluster.center.x);
          mapStore.map.setCenter(center);
          mapStore.map.setZoom(zoomLevel + 1);
        }
      );
    } else {
      // zoomLevel 17+: 개별 식당 마커 표시
      mapStore.clearClusters();
      mapStore.addRestaurantMarkers(
        restaurantStore.getAllRestaurants(),
        (restaurant) => {
          restaurantStore.selectRestaurant(restaurant);
          restaurantStore.fetchRestaurantBusinessInfo(restaurant.restaurantManagementNumber);
        }
      );
    }
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
