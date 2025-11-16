import { markerType } from '../../utils/markerUtils';

/**
 * 순수 DOM 방식으로 마커 요소 생성
 * React root를 생성하지 않아 메모리 효율적
 * @param {string} type - 마커 타입 (markerType의 키)
 * @returns {HTMLElement} 마커 DOM 요소
 */
function MarkerFactory(type) {
  const container = document.createElement('div');
  container.style.cssText = `
    width: 40px;
    height: 40px;
    border-radius: 50%;
    overflow: hidden;
    border: 3px solid #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    background: #fff;
  `;

  const img = document.createElement('img');
  img.src = markerType[type];
  img.alt = 'marker';
  img.style.cssText = `
    width: 100%;
    height: 100%;
    object-fit: cover;
  `;

  container.appendChild(img);
  return container;
}

export default MarkerFactory;
