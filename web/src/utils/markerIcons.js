/**
 * 카테고리별 마커 아이콘 생성 유틸리티
 * 이모지 기반 SVG 마커 생성
 */

// 카테고리별 이모지 매핑
const categoryEmojis = {
  // mainCategory (대분류)
  '한식': '🍚',
  '일식': '🍣',
  '중식': '🥟',
  '양식': '🍝',
  '아시아': '🍜',
  '멕시칸': '🌮',
  '분식': '🍢',
  '도시락': '🍱',
  '치킨': '🍗',
  '술집': '🍺',
  '카페': '☕',
  '뷔페': '🍽️',

  // detailCategory (소분류) - 한식
  '삼겹살': '🥓',
  '곱창': '🫘',
  '갈비': '🥩',
  '불고기': '🥩',
  '김치찌개': '🍲',
  '감자탕': '🍲',
  '삼계탕': '🐔',
  '샤브샤브': '🫕',
  '해물탕': '🦐',
  '사철탕': '🍲',
  '국밥': '🍜',
  '해장국': '🍜',
  '곰탕': '🍜',
  '추어탕': '🐟',
  '설렁탕': '🍜',
  '국수': '🍜',
  '칼국수': '🍜',
  '냉면': '🍜',
  '수제비': '🥟',
  '닭갈비': '🍗',
  '닭강정': '🍗',
  '오리': '🦆',
  '생선구이': '🐟',
  '회': '🍣',
  '장어구이': '🐍',
  '복어': '🐡',
  '조개구이': '🦪',
  '대게찜': '🦀',
  '아구찜': '🐟',
  '족발': '🐷',
  '순대': '🌭',
  '한정식': '🍱',
  '두부': '🧈',
  '죽': '🥣',

  // detailCategory - 일식
  '초밥': '🍣',
  '참치회': '🍣',
  '돈까스': '🍖',
  '라멘': '🍜',
  '오뎅': '🍢',
  '철판요리': '🍳',

  // detailCategory - 중식
  '짜장면': '🍝',
  '양꼬치': '🍢',

  // detailCategory - 양식
  '파스타': '🍝',
  '피자': '🍕',
  '스테이크': '🥩',
  '햄버거': '🍔',
  '샌드위치': '🥪',
  '토스트': '🍞',
  '샐러드': '🥗',

  // detailCategory - 아시아
  '쌀국수': '🍜',
  '팟타이': '🍜',
  '커리': '🍛',
  '케밥': '🥙',

  // detailCategory - 멕시칸
  '타코': '🌮',

  // detailCategory - 분식
  '떡볶이': '🍢',
  '떡': '🍡',

  // detailCategory - 술집
  '칵테일': '🍸',
  '와인': '🍷',

  // detailCategory - 카페
  '커피': '☕',
  '빵': '🥐',
  '도넛': '🍩',
  '아이스크림': '🍦',
  '초콜릿': '🍫',
  '생과일': '🍹',
};

// 기본 이모지
const DEFAULT_EMOJI = '🍴';

/**
 * 카테고리에 해당하는 이모지 반환
 * detailCategory > mainCategory > 기본 순서로 우선순위
 */
export function getCategoryEmoji(mainCategory, detailCategory) {
  if (detailCategory && categoryEmojis[detailCategory]) {
    return categoryEmojis[detailCategory];
  }
  if (mainCategory && categoryEmojis[mainCategory]) {
    return categoryEmojis[mainCategory];
  }
  return DEFAULT_EMOJI;
}

/**
 * 이모지를 포함한 SVG 마커 생성
 */
export function createMarkerSvg(emoji, size = 40) {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
      <circle cx="${size/2}" cy="${size/2}" r="${size/2 - 2}" fill="white" stroke="#333" stroke-width="2"/>
      <text x="${size/2}" y="${size/2 + 1}" font-size="${size * 0.5}" text-anchor="middle" dominant-baseline="central">${emoji}</text>
    </svg>
  `;
  return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg.trim());
}

/**
 * 카테고리 기반 마커 아이콘 URL 생성
 */
export function getMarkerIcon(mainCategory, detailCategory, size = 40) {
  const emoji = getCategoryEmoji(mainCategory, detailCategory);
  return createMarkerSvg(emoji, size);
}

export default {
  getCategoryEmoji,
  createMarkerSvg,
  getMarkerIcon,
};
