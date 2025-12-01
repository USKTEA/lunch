package com.usktea.lunch.service.data

/**
 * 카카오맵 카테고리를 정규화된 분류 체계로 매핑
 */
data class CategoryMapping(
    // 대분류
    val mainCategory: String?,
    // 소분류 (실제 메뉴/요리명)
    val detailCategory: String?,
)

object CategoryMappings {
    // 음식점이 아닌 카테고리 (필터링 대상)
    private val nonRestaurantCategories =
        setOf(
            // 유흥/오락
            "게임방,PC방", "골프연습장", "골프장", "노래방", "녹음실", "나이트,클럽",
            "당구장,포켓볼", "만화카페", "방탈출카페", "볼링장", "스크린골프연습장",
            "스터디카페,스터디룸", "찜질방", "태권도장", "복싱,권투",
            // 숙박
            "호텔", "빌라,주택", "여관,모텔", "펜션", "산후조리원",
            // 의료/건강
            "내과", "정신건강의학과", "치과", "피부과", "성형외과", "일반의원",
            "약국", "체형관리", "헬스클럽", "의료기기판매",
            // 미용
            "미용실", "미용", "미용용품",
            // 교육
            "학원", "영어학원", "음악학원", "미술학원", "피아노학원", "요리학원",
            "서예,한문학원", "고등학교", "어린이집", "무용,댄스",
            // 부동산/건설
            "부동산서비스", "건설,건축", "시공업체", "종합건설사",
            // 금융/법률
            "보험사", "저축은행", "자산관리,자산운용", "법률,행정", "세무,회계", "관세사",
            // 판매점
            "의류판매", "남성의류,양복", "여성의류", "아동복,유아복", "속옷,언더웨어",
            "화장품", "보석,귀금속", "가구판매", "가방", "가정,생활", "생활용품점",
            "패션잡화점", "패션", "안경,렌즈", "시계판매", "조명기기판매",
            "전자제품렌탈", "컴퓨터판매", "자동차판매점", "과일,채소가게",
            "슈퍼마켓", "편의점", "드럭스토어", "면세점", "꽃집,꽃배달",
            // 문화/예술
            "박물관", "미술관", "영화관", "갤러리카페", "문화시설", "문화예술단체",
            "관광,명소", "도시근린공원", "광장", "산", "수족관",
            // 사업/서비스
            "기업", "빌딩", "서비스,산업", "소프트웨어", "컨설팅", "관리,운영",
            "광고기획,광고대행", "이벤트기획,대행", "번역,통역", "경비,경호",
            "청소대행", "방송프로그램제작", "연예기획사", "공유오피스", "공간대여",
            // 제조/도매
            "제조업", "식품가공,제조", "주류제조", "주류도매,주류유통", "공구,공작기계",
            "가죽공예",
            // 기타
            "화방", "서점", "사진관,포토스튜디오", "대여사진관", "방앗간",
            "복권", "주차장", "담배", "통신판매", "인터넷쇼핑몰", "기념품판매",
            "예식관련기타", "예식장", "결혼", "웨딩드레스", "산모도우미",
            "여행사", "카인테리어", "주방용품",
        )

    // 음식점 카테고리 매핑
    private val mappings =
        mapOf(
            // 한식
            "한식" to CategoryMapping("한식", null),
            "육류,고기" to CategoryMapping("한식", "삼겹살"),
            "삼겹살" to CategoryMapping("한식", "삼겹살"),
            "곱창,막창" to CategoryMapping("한식", "곱창"),
            "갈비" to CategoryMapping("한식", "갈비"),
            "불고기,두루치기" to CategoryMapping("한식", "불고기"),
            "정육점" to CategoryMapping("한식", null),
            "찌개,전골" to CategoryMapping("한식", "김치찌개"),
            "감자탕" to CategoryMapping("한식", "감자탕"),
            "삼계탕" to CategoryMapping("한식", "삼계탕"),
            "샤브샤브" to CategoryMapping("한식", "샤브샤브"),
            "매운탕,해물탕" to CategoryMapping("한식", "해물탕"),
            "사철탕,영양탕" to CategoryMapping("한식", "사철탕"),
            "국밥" to CategoryMapping("한식", "국밥"),
            "해장국" to CategoryMapping("한식", "해장국"),
            "곰탕" to CategoryMapping("한식", "곰탕"),
            "추어" to CategoryMapping("한식", "추어탕"),
            "설렁탕" to CategoryMapping("한식", "설렁탕"),
            "국수" to CategoryMapping("한식", "국수"),
            "칼국수" to CategoryMapping("한식", "칼국수"),
            "냉면" to CategoryMapping("한식", "냉면"),
            "수제비" to CategoryMapping("한식", "수제비"),
            "닭요리" to CategoryMapping("한식", "닭갈비"),
            "닭강정" to CategoryMapping("한식", "닭강정"),
            "오리" to CategoryMapping("한식", "오리"),
            "해물,생선" to CategoryMapping("한식", "생선구이"),
            "회" to CategoryMapping("한식", "회"),
            "장어" to CategoryMapping("한식", "장어구이"),
            "복어" to CategoryMapping("한식", "복어"),
            "조개" to CategoryMapping("한식", "조개구이"),
            "게,대게" to CategoryMapping("한식", "대게찜"),
            "아구" to CategoryMapping("한식", "아구찜"),
            "해산물" to CategoryMapping("한식", null),
            "수산물판매" to CategoryMapping("한식", null),
            "족발,보쌈" to CategoryMapping("한식", "족발"),
            "순대" to CategoryMapping("한식", "순대"),
            "한정식" to CategoryMapping("한식", "한정식"),
            "한식뷔페" to CategoryMapping("한식", null),
            "두부전문점" to CategoryMapping("한식", "두부"),
            "퓨전한식" to CategoryMapping("한식", null),
            "죽" to CategoryMapping("한식", "죽"),
            // 일식
            "일식" to CategoryMapping("일식", null),
            "일식집" to CategoryMapping("일식", null),
            "초밥,롤" to CategoryMapping("일식", "초밥"),
            "참치회" to CategoryMapping("일식", "참치회"),
            "돈까스,우동" to CategoryMapping("일식", "돈까스"),
            "일본식라면" to CategoryMapping("일식", "라멘"),
            "일본식주점" to CategoryMapping("일식", null),
            "실내포장마차" to CategoryMapping("일식", null),
            "오뎅바" to CategoryMapping("일식", "오뎅"),
            "퓨전일식" to CategoryMapping("일식", null),
            "철판요리" to CategoryMapping("일식", "철판요리"),
            // 중식
            "중식" to CategoryMapping("중식", null),
            "중국요리" to CategoryMapping("중식", "짜장면"),
            "양꼬치" to CategoryMapping("중식", "양꼬치"),
            // 양식
            "양식" to CategoryMapping("양식", null),
            "이탈리안" to CategoryMapping("양식", "파스타"),
            "피자" to CategoryMapping("양식", "피자"),
            "스테이크,립" to CategoryMapping("양식", "스테이크"),
            "햄버거" to CategoryMapping("양식", "햄버거"),
            "샌드위치" to CategoryMapping("양식", "샌드위치"),
            "패스트푸드" to CategoryMapping("양식", null),
            "토스트" to CategoryMapping("양식", "토스트"),
            "샐러드" to CategoryMapping("양식", "샐러드"),
            "프랑스음식" to CategoryMapping("양식", null),
            "패밀리레스토랑" to CategoryMapping("양식", null),
            // 아시아
            "베트남음식" to CategoryMapping("아시아", "쌀국수"),
            "태국음식" to CategoryMapping("아시아", "팟타이"),
            "동남아음식" to CategoryMapping("아시아", null),
            "아시아음식" to CategoryMapping("아시아", null),
            "인도음식" to CategoryMapping("아시아", "커리"),
            "튀르키예음식" to CategoryMapping("아시아", "케밥"),
            // 멕시칸
            "멕시칸,브라질" to CategoryMapping("멕시칸", "타코"),
            // 분식
            "분식" to CategoryMapping("분식", null),
            "떡볶이" to CategoryMapping("분식", "떡볶이"),
            "떡,한과" to CategoryMapping("분식", "떡"),
            // 도시락
            "도시락" to CategoryMapping("도시락", "도시락"),
            "배달도시락" to CategoryMapping("도시락", "도시락"),
            // 치킨
            "치킨" to CategoryMapping("치킨", "치킨"),
            // 술집
            "호프,요리주점" to CategoryMapping("술집", null),
            "술집" to CategoryMapping("술집", null),
            "칵테일바" to CategoryMapping("술집", "칵테일"),
            "와인바" to CategoryMapping("술집", "와인"),
            "유흥주점" to CategoryMapping("술집", null),
            "유흥시설" to CategoryMapping("술집", null),
            "라이브카페" to CategoryMapping("술집", null),
            // 카페
            "카페" to CategoryMapping("카페", null),
            "커피전문점" to CategoryMapping("카페", "커피"),
            "디저트카페" to CategoryMapping("카페", null),
            "테마카페" to CategoryMapping("카페", null),
            "애견카페" to CategoryMapping("카페", null),
            "보드카페" to CategoryMapping("카페", null),
            "북카페" to CategoryMapping("카페", null),
            "전통찻집" to CategoryMapping("카페", null),
            "차,커피" to CategoryMapping("카페", null),
            "다방" to CategoryMapping("카페", null),
            "제과,베이커리" to CategoryMapping("카페", "빵"),
            "도넛" to CategoryMapping("카페", "도넛"),
            "아이스크림" to CategoryMapping("카페", "아이스크림"),
            "아이스크림판매" to CategoryMapping("카페", "아이스크림"),
            "초콜릿" to CategoryMapping("카페", "초콜릿"),
            "간식" to CategoryMapping("카페", null),
            "생과일전문점" to CategoryMapping("카페", "생과일"),
            // 뷔페
            "뷔페" to CategoryMapping("뷔페", null),
            "푸드코트" to CategoryMapping("뷔페", null),
            // 기타 (전부 null)
            "퓨전요리" to CategoryMapping(null, null),
            "음식점" to CategoryMapping(null, null),
            "출장요리" to CategoryMapping(null, null),
            "구내식당" to CategoryMapping(null, null),
            "기사식당" to CategoryMapping(null, null),
            "채선당" to CategoryMapping(null, null),
            "식품" to CategoryMapping(null, null),
            "식품판매" to CategoryMapping(null, null),
            "식품서비스업" to CategoryMapping(null, null),
            "반찬가게" to CategoryMapping(null, null),
            "주류판매" to CategoryMapping(null, null),
        )

    /**
     * 카카오맵 카테고리를 정규화된 분류로 매핑
     */
    fun findMapping(kakaoCategory: String): CategoryMapping? {
        return mappings[kakaoCategory]
    }

    /**
     * 음식점 여부 확인
     */
    fun isRestaurant(kakaoCategory: String): Boolean {
        if (nonRestaurantCategories.contains(kakaoCategory)) {
            return false
        }
        return mappings.containsKey(kakaoCategory)
    }

    /**
     * 여러 카테고리 중 하나라도 음식점이면 true
     */
    fun hasRestaurantCategory(kakaoCategories: Set<String>): Boolean {
        return kakaoCategories.any { isRestaurant(it) }
    }

    /**
     * 카테고리 목록에서 첫 번째로 매핑되는 음식점 카테고리 반환
     * mainCategory가 있는 매핑을 우선 반환
     */
    fun findFirstRestaurantMapping(kakaoCategories: Set<String>): CategoryMapping? {
        val withMain =
            kakaoCategories
                .filter { isRestaurant(it) }
                .mapNotNull { findMapping(it) }
                .firstOrNull { it.mainCategory != null }

        if (withMain != null) {
            return withMain
        }

        return kakaoCategories
            .filter { isRestaurant(it) }
            .firstNotNullOfOrNull { findMapping(it) }
    }
}
