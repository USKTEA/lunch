/* global naver */

import Store from './Store';
import MarkerFactory from '../components/ui/MarkerFactory';
import { getMarkerIcon } from '../utils/markerIcons';

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

  // H3 클러스터 폴리곤 및 마커
  clusterPolygons = [];
  clusterMarkers = [];

  constructor() {
    super();
  }

  /**
   * 네이버 지도 초기화
   * @param {string} elementId - 지도를 렌더링할 DOM 요소 ID
   * @param {object} position - 현재 위치
   */
  initializeMap(elementId, position = this.defaultPosition) {
    if (this.map) {
      return;
    }

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
        // 카테고리 기반 마커 아이콘 생성
        const markerIconUrl = getMarkerIcon(
          restaurant.mainCategory,
          restaurant.detailCategory,
          40
        );

        const marker = new naver.maps.Marker({
          position: adjustedPos,
          map: this.map,
          icon: {
            url: markerIconUrl,
            size: new naver.maps.Size(40, 40),
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

  /**
   * H3 클러스터 폴리곤 및 개수 마커 표시
   * @param {Array} clusters - 클러스터 배열 [{h3Index, center, boundary, restaurants}]
   * @param {Function} onClusterClick - 클러스터 클릭 콜백
   */
  addClusterPolygons(clusters, onClusterClick) {
    if (!this.map) {
      console.error('Map is not initialized');
      return;
    }

    // 기존 클러스터 정리
    this.clearClusters();

    clusters.forEach(cluster => {
      const count = cluster.restaurants.length;
      const maxCount = 200;
      // 개수에 따른 폴리곤 투명도 계산 (0.15 ~ 0.6)
      const fillOpacity = 0.15 + (Math.min(count, maxCount) / maxCount) * 0.45;

      // 폴리곤 경계 좌표 변환
      const paths = cluster.boundary.map(
        coord => new naver.maps.LatLng(coord.y, coord.x)
      );

      // 폴리곤 생성
      const polygon = new naver.maps.Polygon({
        map: this.map,
        paths: paths,
        fillColor: '#E50914',
        fillOpacity: fillOpacity,
        strokeColor: '#B20710',
        strokeWeight: 2,
        clickable: true,
      });

      // 클릭 이벤트
      if (onClusterClick) {
        naver.maps.Event.addListener(polygon, 'click', () => {
          onClusterClick(cluster);
        });
      }

      this.clusterPolygons.push(polygon);

      // 셀 중앙에 개수 표시 마커 (원형, 불투명)
      // 개수에 따른 원 크기 (36px ~ 56px)
      const size = 36 + (Math.min(count, maxCount) / maxCount) * 20;

      const countMarker = new naver.maps.Marker({
        position: new naver.maps.LatLng(cluster.center.y, cluster.center.x),
        map: this.map,
        icon: {
          content: `<div style="
            background: #E03030;
            color: white;
            width: ${size}px;
            height: ${size}px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            font-size: ${count >= 100 ? 12 : 14}px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
            border: 2px solid rgba(255,255,255,0.8);
          ">${count}</div>`,
          anchor: new naver.maps.Point(size / 2, size / 2),
        },
        clickable: true,
      });

      // 마커 클릭 이벤트
      if (onClusterClick) {
        naver.maps.Event.addListener(countMarker, 'click', () => {
          onClusterClick(cluster);
        });
      }

      this.clusterMarkers.push(countMarker);
    });

    this.publish();
  }

  /**
   * 클러스터 폴리곤 및 마커 정리
   */
  clearClusters() {
    this.clusterPolygons.forEach(p => p.setMap(null));
    this.clusterMarkers.forEach(m => m.setMap(null));
    this.clusterPolygons = [];
    this.clusterMarkers = [];
  }

  /**
   * 식당 마커 정리 (회사 마커 제외)
   */
  clearRestaurantMarkers() {
    const companyMarker = this.markers[0];
    this.markers.slice(1).forEach(m => m.setMap(null));
    this.markers = [companyMarker];
  }
}

export const mapStore = new MapStore();
