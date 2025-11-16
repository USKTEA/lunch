/* global naver */

import Store from './Store';
import MarkerFactory from '../components/ui/MarkerFactory';

// Naver Map Wrapper
class MapStore extends Store {
  // 서울특별시 강남구 역삼1동 654-3
  defaultPosition = {
    y: 37.507017,
    x: 127.039638,
  };

  // 30m
  defaultZoomLevel = 18;

  map = null;

  markers = [];

  constructor() {
    super();
  }

  /**
   * 네이버 지도 초기화
   * @param {string} elementId - 지도를 렌더링할 DOM 요소 ID
   * @param {object} position - 현재 위치
   */
  initializeMap(elementId, position = this.defaultPosition) {
    this.defaultPosition = position;

    const mapOptions = {
      center: new naver.maps.LatLng(position.y, position.x),
      zoom: this.defaultZoomLevel,
      minZoom: 16,
      maxZoom: 19,
      zoomControl: true,
    };

    this.map = new naver.maps.Map(elementId, mapOptions);

    const markerOptions = {
      position: new naver.maps.LatLng(position.y, position.x),
      map: this.map,
      icon: {
        content: MarkerFactory('byb'),
        anchor: new naver.maps.Point(20, 20),
      },
      zIndex: 300
    };

    this.markers = [new naver.maps.Marker(markerOptions)];
    this.publish();
  }

  addRestaurantMarkers(restaurants, onMarkerClick) {
    if (!this.map) {
      console.error('Map is not initialized');
      return;
    }

    const companyMarker = this.markers[0];
    const existingMarkers = this.markers.slice(1);

    const existingMarkersMap = new Map(
      existingMarkers.map((marker) => {
        const pos = marker.getPosition();
        const key = `${pos.x}_${pos.y}`;
        return [key, marker];
      })
    );

    const newRestaurantsMap = new Map();
    restaurants.forEach((restaurant) => {
      const adjustedPos = new naver.maps.LatLng(
        restaurant.coordinate.y,
        restaurant.coordinate.x
      );
      const key = `${adjustedPos.x}_${adjustedPos.y}`;
      newRestaurantsMap.set(key, { restaurant, adjustedPos });
    });

    existingMarkersMap.forEach((marker, key) => {
      if (!newRestaurantsMap.has(key)) {
        marker.setMap(null);
        existingMarkersMap.delete(key);
      }
    });

    const newMarkers = [];
    newRestaurantsMap.forEach(({ restaurant, adjustedPos }, key) => {
      if (!existingMarkersMap.has(key)) {
        const marker = new naver.maps.Marker({
          position: adjustedPos,
          map: this.map,
          icon: {
            content: MarkerFactory('restaurant'),
            anchor: new naver.maps.Point(20, 20),
          },
        });

        // 마커 클릭 이벤트 리스너 추가
        if (onMarkerClick) {
          naver.maps.Event.addListener(marker, 'click', () => {
            onMarkerClick(restaurant);
          });
        }

        newMarkers.push(marker);
      }
    });

    this.markers = [
      companyMarker,
      ...Array.from(existingMarkersMap.values()),
      ...newMarkers,
    ];

    this.publish();
  }

  currentBoundary() {
    const bounds = this.map.getBounds();

    return {
      southWestBoundary: {
        x: bounds.getSW().x,
        y: bounds.getSW().y,
      },
      northEastBoundary: {
        x: bounds.getNE().x,
        y: bounds.getNE().y,
      },
    };
  }

  currentZoomLevel() {
    return this.map.getZoom();
  }
}

export const mapStore = new MapStore();
