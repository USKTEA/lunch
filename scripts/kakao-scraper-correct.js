/**
 * 카카오맵 정확한 스크래핑
 * - 실제 HTML 구조 기반
 */

const { chromium } = require('playwright');
const XLSX = require('xlsx');
const fs = require('fs');

// 롤 형용사와 챔피언 이름 리스트
const lolAdjectives = [
  '강력한', '빠른', '용맹한', '지혜로운', '암흑의', '신성한', '불굴의', '전설의', '고귀한', '야만의',
  '배고픈', '졸린', '화난', '행복한', '슬픈', '귀여운', '무서운', '춤추는', '노래하는', '웃긴',
  '엉뚱한', '똑똑한', '바보같은', '멋진', '우아한', '시끄러운', '조용한', '수상한', '신비로운', '당황한',
  '흥분한', '지친', '깨어난', '날아가는', '뛰어가는', '기어가는', '구르는', '점프하는', '펄떡이는', '돌진하는'
];
const lolChampions = ['아리', '야스오', '진', '럭스', '이즈리얼', '케이틀린', '블리츠크랭크', '리신', '쓰레쉬', '제드', '아칼리', '카타리나', '갱플랭크', '트위스티드페이트', '애쉬'];

function generateRandomAuthor() {
  const adj = lolAdjectives[Math.floor(Math.random() * lolAdjectives.length)];
  const champ = lolChampions[Math.floor(Math.random() * lolChampions.length)];
  return `${adj} ${champ}`;
}

// 타임아웃 래퍼 함수
async function withTimeout(promise, timeoutMs, defaultValue = null) {
  const timeout = new Promise((resolve) => {
    setTimeout(() => resolve(defaultValue), timeoutMs);
  });
  return Promise.race([promise, timeout]);
}

async function scrapePlaceInfo(address, placeName, managementNumber, workerId) {
  console.log(`[Worker ${workerId}] 🔍 검색: ${address} ${placeName}\n`);

  const browser = await chromium.launch({
    headless: true
  });

  const context = await browser.newContext({
    locale: 'ko-KR',
    timezoneId: 'Asia/Seoul',
    viewport: { width: 1400, height: 900 }
  });

  const page = await context.newPage();

  try {
    // 1. 검색 후 상세페이지 이동
    console.log('1️⃣ 카카오맵 검색...\n');
    await page.goto('https://map.kakao.com/');
    await page.waitForTimeout(960);

    const searchBox = await page.locator('#search\\.keyword\\.query');
    await searchBox.fill(`${address} ${placeName}`);
    await searchBox.press('Enter');
    await page.waitForTimeout(640);

    // 검색 결과 확인
    const firstResult = await page.locator('#info\\.search\\.place\\.list > li').first();
    let isResultVisible = await firstResult.isVisible({ timeout: 2000 }).catch(() => false);

    if (!isResultVisible) {
      console.log('⚠️  검색 결과 없음, 구 단위로 재검색 시도...\n');

      // 주소에서 구 추출 (예: "서울특별시 서초구 ..." -> "서초구")
      const addressParts = address.split(' ');
      if (addressParts.length >= 2) {
        const district = addressParts[1]; // 서초구, 강남구 등
        console.log(`🔄 ${district} ${placeName}로 재검색...\n`);

        await searchBox.clear();
        await searchBox.fill(`${district} ${placeName}`);
        await searchBox.press('Enter');
        await page.waitForTimeout(640);

        isResultVisible = await firstResult.isVisible({ timeout: 2000 }).catch(() => false);

        if (!isResultVisible) {
          console.log('⚠️  구 단위 재검색 결과 없음, 식당이름만으로 재검색 시도...\n');

          console.log(`🔄 ${placeName}로 재검색...\n`);

          await searchBox.clear();
          await searchBox.fill(placeName);
          await searchBox.press('Enter');
          await page.waitForTimeout(640);

          isResultVisible = await firstResult.isVisible({ timeout: 2000 }).catch(() => false);

          if (!isResultVisible) {
            console.log('⚠️  모든 재검색 실패\n');
            return null;
          }

          console.log('✅ 식당이름 검색 성공!\n');
        } else {
          console.log('✅ 구 단위 재검색 성공!\n');
        }
      } else {
        console.log('⚠️  주소 형식이 올바르지 않음, 식당이름만으로 검색...\n');

        await searchBox.clear();
        await searchBox.fill(placeName);
        await searchBox.press('Enter');
        await page.waitForTimeout(640);

        isResultVisible = await firstResult.isVisible({ timeout: 2000 }).catch(() => false);

        if (!isResultVisible) {
          console.log('⚠️  모든 재검색 실패\n');
          return null;
        }

        console.log('✅ 식당이름 검색 성공!\n');
      }
    }

    const moreViewLink = await firstResult.locator('a[data-id="moreview"]').first();
    const detailUrl = await moreViewLink.getAttribute('href');

    console.log(`📍 ${detailUrl}\n`);

    await page.goto(detailUrl, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    });
    await page.waitForTimeout(640);

    const result = {
      restaurantManagementNumber: managementNumber,
      url: detailUrl
    };

    // 식당 이름 추출
    try {
      const nameEl = await page.locator('.tit_place').first();
      if (await nameEl.isVisible({ timeout: 1000 })) {
        const name = await nameEl.textContent();
        result.name = name.trim();
        console.log(`✅ 식당명: ${result.name}\n`);
      }
    } catch (e) {}

    // 2. 탭 확인
    console.log('2️⃣ 탭 확인...\n');
    const tabs = await page.locator('a.link_tab[role="tab"]').all();
    const tabNames = [];
    for (const tab of tabs) {
      const text = await tab.textContent();
      tabNames.push(text.trim());
    }
    console.log(`✅ 탭: ${tabNames.join(', ')}\n`);

    const hasReviews = tabNames.includes('후기');
    console.log(`✅ 후기 제공: ${hasReviews ? '있음' : '없음'}\n`);

    // 3. 홈 탭 - 카테고리, 운영시간, 연락처 (최대 1분)
    console.log('3️⃣ 홈 탭 정보...\n');
    await withTimeout(
      (async () => {
        const homeTab = page.locator('a.link_tab[href="#home"]').first();
        if (await homeTab.isVisible({ timeout: 2000 })) {
          await homeTab.click();
          await page.waitForTimeout(400);

          // 카테고리
          try {
            const categoryEl = await page.locator('.info_cate').first();
            if (await categoryEl.isVisible({ timeout: 2000 })) {
              const categoryText = await categoryEl.textContent();
              // "장소 카테고리회" -> "회" 추출
              const category = categoryText.replace('장소 카테고리', '').trim();
              if (category) {
                result.category = category;
                console.log(`✅ 카테고리: ${category}`);
              }
            }
          } catch (e) {}

          // AI 요약
          try {
            const summaryEl = await page.locator('.ai_info .txt_option').first();
            if (await summaryEl.isVisible({ timeout: 1000 })) {
              const summary = await summaryEl.textContent();
              if (summary && summary.trim()) {
                result.summary = summary.trim();
                console.log(`✅ AI 요약: ${summary.trim()}`);
              }
            }
          } catch (e) {}

          // 연락처
          try {
            const phoneElements = await page.locator('.txt_detail').all();
            for (const phoneEl of phoneElements) {
              const text = await phoneEl.textContent();
              const phoneMatch = text.match(/^(\d{2,3}-\d{3,4}-\d{4})$/);
              if (phoneMatch) {
                result.phone = phoneMatch[1];
                console.log(`✅ 연락처: ${result.phone}`);
                break;
              }
            }
          } catch (e) {}

          // 요일별 운영시간
          try {
            // "영업시간을 알려주세요"가 있으면 영업시간 정보 없음
            const bodyText = await page.textContent('body');
            if (bodyText.includes('영업시간을 알려주세요')) {
              console.log(`⚠️  영업시간 정보 없음`);
            } else {
              // 펼치기 버튼 클릭
              const foldButton = page.locator('.btn_fold2').first();
              if (await foldButton.isVisible({ timeout: 1000 })) {
                await foldButton.click();
                await page.waitForTimeout(160);
              }

              // 요일별 시간 파싱
              const lineFolds = await page.locator('.fold_detail .line_fold').all();
              const businessHours = [];

              for (const lineFold of lineFolds) {
                try {
                  const dayEl = await lineFold.locator('.tit_fold').first();
                  const dayText = await dayEl.textContent();

                  // 모든 txt_detail 요소 가져오기 (영업시간 + 브레이크타임)
                  const timeElements = await lineFold.locator('.detail_fold .txt_detail').all();

                  // 날짜 제거하고 요일만 추출 (예: "토(11/22)" -> "토")
                  const dayOnly = dayText.trim().replace(/\(.*?\)/, '');

                  // 요일을 영문 대문자로 변환
                  const dayMap = {
                    '월': 'MON',
                    '화': 'TUE',
                    '수': 'WED',
                    '목': 'THU',
                    '금': 'FRI',
                    '토': 'SAT',
                    '일': 'SUN'
                  };
                  const dayInEnglish = dayMap[dayOnly] || dayOnly;

                  if (timeElements.length === 0) continue;

                  // 첫 번째는 영업시간
                  const mainTime = await timeElements[0].textContent();
                  const mainTimeText = mainTime.trim();
                  const isClosedDay = mainTimeText === '휴무일' || mainTimeText.includes('휴무');

                  let openAt = null;
                  let closeAt = null;
                  let breakTimeStart = null;
                  let breakTimeEnd = null;

                  if (!isClosedDay) {
                    // "11:00 ~ 22:00" 형식에서 시간 추출
                    const timeMatch = mainTimeText.match(/(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})/);
                    if (timeMatch) {
                      openAt = timeMatch[1];
                      closeAt = timeMatch[2];
                    }

                    // 두 번째가 있으면 브레이크타임
                    if (timeElements.length > 1) {
                      const breakTime = await timeElements[1].textContent();
                      const breakTimeText = breakTime.trim();

                      // "14:50 ~ 16:00 브레이크타임" 형식에서 시간 추출
                      const breakMatch = breakTimeText.match(/(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})/);
                      if (breakMatch) {
                        breakTimeStart = breakMatch[1];
                        breakTimeEnd = breakMatch[2];
                      }
                    }
                  }

                  const hourData = {
                    day: dayInEnglish,
                    openAt: openAt,
                    closeAt: closeAt,
                    isOpen: !isClosedDay
                  };

                  // 브레이크타임이 있으면 추가
                  if (breakTimeStart && breakTimeEnd) {
                    hourData.breakTimeStart = breakTimeStart;
                    hourData.breakTimeEnd = breakTimeEnd;
                  }

                  businessHours.push(hourData);
                } catch (e) {}
              }

              if (businessHours.length > 0) {
                result.businessHours = businessHours;
                console.log(`✅ 운영시간 (${businessHours.length}일):`);
                businessHours.forEach(item => {
                  const displayHours = item.isOpen ? `${item.openAt} ~ ${item.closeAt}` : '휴무일';
                  console.log(`   ${item.day}: ${displayHours}`);
                });
              }
            }
          } catch (e) {}

          console.log('');
        }
      })(),
      60000
    );

    // 4. 메뉴 탭 (최대 1분)
    console.log('4️⃣ 메뉴 탭 정보...\n');
    await withTimeout(
      (async () => {
        const menuTab = page.locator('a.link_tab[href="#menuInfo"]').first();
        if (await menuTab.isVisible({ timeout: 2000 })) {
          await menuTab.click();
          await page.waitForTimeout(960);

          const menuItems = await page.locator('ul.list_goods > li').all();
          const menus = [];

          // 최대 10개만 가져오기
          for (let i = 0; i < Math.min(10, menuItems.length); i++) {
            const item = menuItems[i];
            try {
              // 메뉴명
              const nameEl = await item.locator('.tit_item').first();
              const name = await nameEl.textContent();

              // 가격
              const priceEl = await item.locator('.desc_item').first();
              const priceText = await priceEl.textContent();
              // "11,500원" -> 11500 (숫자로 변환)
              const price = parseInt(priceText.replace(/[,원]/g, ''));

              // 대표 태그 확인
              const badges = await item.locator('.badge_label').all();
              let isRepresentative = false;
              for (const badge of badges) {
                const badgeText = await badge.textContent();
                if (badgeText.includes('대표')) {
                  isRepresentative = true;
                  break;
                }
              }

              menus.push({
                name: name.trim(),
                price: price,
                isRepresentative: isRepresentative
              });
            } catch (e) {}
          }

          if (menus.length > 0) {
            result.menus = menus;
            console.log(`✅ 메뉴 (${menus.length}개):`);
            menus.slice(0, 5).forEach((menu, idx) => {
              const badge = menu.isRepresentative ? ' [대표]' : '';
              console.log(`   ${idx + 1}. ${menu.name} - ${menu.price}${badge}`);
            });
            if (menus.length > 5) {
              console.log(`   ... 외 ${menus.length - 5}개`);
            }
            console.log('');
          } else {
            console.log('⚠️  메뉴 정보 없음\n');
          }
        } else {
          console.log('⚠️  메뉴 탭 없음\n');
        }
      })(),
      60000
    );

    // 5. 후기 탭 (후기 제공 식당만, 최대 1분)
    if (hasReviews) {
      console.log('5️⃣ 후기 탭 정보...\n');
      await withTimeout(
        (async () => {
          const reviewTab = page.locator('a.link_tab[href="#review"]').first();
          if (await reviewTab.isVisible({ timeout: 2000 })) {
            await reviewTab.click();
            await page.waitForTimeout(960);

            // 평점 (콘솔 출력용)
            let displayRating = null;
            try {
              const ratingEl = await page.locator('.num_star').first();
              const rating = await ratingEl.textContent();
              displayRating = parseFloat(rating.trim());
              console.log(`✅ 평점: ${displayRating}/5.0`);
            } catch (e) {}

            // 후기 수 (콘솔 출력용)
            let displayReviewCount = null;
            try {
              const reviewCountEl = await page.locator('.tit_total').first();
              const reviewCountText = await reviewCountEl.textContent();
              const match = reviewCountText.match(/후기\s*(\d+)/);
              if (match) {
                displayReviewCount = parseInt(match[1]);
                console.log(`✅ 후기 수: ${displayReviewCount}개`);
              }
            } catch (e) {}

            // 후기 목록
            const reviewItems = await page.locator('ul.list_review > li').all();
            const reviews = [];

            console.log(`\n📝 후기 (최대 5개):\n`);

            for (let i = 0; i < Math.min(5, reviewItems.length); i++) {
              try {
                const item = reviewItems[i];

                // 작성자 - 롤 형용사 + 챔피언 이름으로 익명화
                const anonymousAuthor = generateRandomAuthor();

                // 날짜 - "2024.10.28." -> "2024-10-28" 형식으로 변환
                const dateEl = await item.locator('.txt_date').first();
                const dateText = await dateEl.textContent();
                // "2024.10.28." -> "2024-10-28"
                const formattedDate = dateText.trim().replace(/\./g, '-').replace(/-$/, '');

                // 별점 (hidden text로 되어 있음)
                const stars = await item.locator('.starred_grade .screen_out').all();
                let starRating = null;
                for (const star of stars) {
                  const text = await star.textContent();
                  const ratingMatch = text.match(/^([0-9.]+)$/);
                  if (ratingMatch) {
                    starRating = parseFloat(ratingMatch[1]);
                    break;
                  }
                }

                // 리뷰 내용
                const contentEl = await item.locator('.desc_review').first();
                const content = await contentEl.textContent();

                const review = {
                  author: anonymousAuthor,
                  date: formattedDate,
                  rating: starRating,
                  content: content.trim()
                };

                reviews.push(review);

                console.log(`${i + 1}. ⭐ ${review.rating}/5 - ${review.author} (${review.date})`);
                console.log(`   "${review.content.substring(0, 100)}${review.content.length > 100 ? '...' : ''}"\n`);

              } catch (e) {
                console.log(`${i + 1}. 파싱 실패\n`);
              }
            }

            if (reviews.length > 0) {
              result.reviews = reviews;
            }

            console.log('');
          }
        })(),
        60000
      );
    }

    console.log('⏰ 0.96초 대기...\n');
    await page.waitForTimeout(960);

    return result;

  } catch (error) {
    console.error('❌ 에러:', error.message);
    throw error;
  } finally {
    await browser.close();
  }
}

// Excel 파일에서 데이터 읽기
function readExcelFile(filePath) {
  console.log(`📂 Excel 파일 읽기: ${filePath}\n`);

  const workbook = XLSX.readFile(filePath);
  const sheetName = workbook.SheetNames[0];
  const worksheet = workbook.Sheets[sheetName];

  // 데이터를 배열로 변환 (헤더 포함)
  const data = XLSX.utils.sheet_to_json(worksheet, { header: 1 });

  // 첫 행은 헤더이므로 제외하고 나머지 데이터만 추출
  const restaurants = [];
  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    if (row[0] && row[1] && row[2]) { // management_number, address, name이 모두 있는 경우만
      restaurants.push({
        managementNumber: row[0],
        address: row[1],
        name: row[2]
      });
    }
  }

  console.log(`✅ 총 ${restaurants.length}개 식당 데이터 로드 완료\n`);
  return restaurants;
}

// 워커 함수 - 각 워커가 자신의 할당된 식당들을 처리
async function worker(workerId, restaurants) {
  const results = [];
  let successCount = 0;
  let failCount = 0;

  console.log(`\n[Worker ${workerId}] 🚀 ${restaurants.length}개 식당 처리 시작\n`);

  for (let i = 0; i < restaurants.length; i++) {
    const restaurant = restaurants[i];
    console.log(`\n[Worker ${workerId}][${i + 1}/${restaurants.length}] 처리 중`);
    console.log(`관리번호: ${restaurant.managementNumber}`);
    console.log(`주소: ${restaurant.address}`);
    console.log(`이름: ${restaurant.name}\n`);

    try {
      const result = await scrapePlaceInfo(
        restaurant.address,
        restaurant.name,
        restaurant.managementNumber,
        workerId
      );

      if (result) {
        results.push(result);
        successCount++;
        console.log(`[Worker ${workerId}] ✅ 성공\n`);
      } else {
        failCount++;
        console.log(`[Worker ${workerId}] ⚠️  검색 결과 없음\n`);
      }
    } catch (error) {
      failCount++;
      console.log(`[Worker ${workerId}] ❌ 에러: ${error.message}\n`);
    }

    // 진행 상황
    console.log(`[Worker ${workerId}] 📊 ${i + 1}/${restaurants.length} (성공: ${successCount}, 실패: ${failCount})\n`);
    console.log('='.repeat(80) + '\n');
  }

  // 각 워커의 결과를 개별 파일로 저장
  const outputFilePath = `./kakao-scraping-results-worker-${workerId}.json`;
  fs.writeFileSync(outputFilePath, JSON.stringify(results, null, 2), 'utf8');

  console.log(`\n[Worker ${workerId}] 🎉 완료! (성공: ${successCount}, 실패: ${failCount})`);
  console.log(`[Worker ${workerId}] 📄 저장: ${outputFilePath}\n`);

  return { workerId, successCount, failCount, results };
}

// 실행
(async () => {
  const excelFilePath = './lunch_lunch_restaurant.xlsx';
  const allRestaurants = readExcelFile(excelFilePath);

  // 모든 식당 처리
  const restaurants = allRestaurants;
  console.log(`\n🎯 전체 ${allRestaurants.length}개 식당 처리 시작\n`);

  // 10개 워커로 나누기
  const workerCount = 10;
  const chunkSize = Math.ceil(restaurants.length / workerCount);
  const chunks = [];

  for (let i = 0; i < workerCount; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize, restaurants.length);
    chunks.push(restaurants.slice(start, end));
  }

  console.log(`\n🚀 ${workerCount}개 워커로 병렬 처리 시작\n`);
  chunks.forEach((chunk, i) => {
    console.log(`Worker ${i + 1}: ${chunk.length}개 식당`);
  });
  console.log('\n' + '='.repeat(80) + '\n');

  // 병렬 실행
  const startTime = Date.now();
  const workerPromises = chunks.map((chunk, i) => worker(i + 1, chunk));
  const workerResults = await Promise.all(workerPromises);

  const endTime = Date.now();
  const totalTime = ((endTime - startTime) / 1000 / 60).toFixed(2);

  // 전체 결과 집계
  let totalSuccess = 0;
  let totalFail = 0;
  workerResults.forEach(wr => {
    totalSuccess += wr.successCount;
    totalFail += wr.failCount;
  });

  console.log('\n' + '='.repeat(80));
  console.log('🎉 전체 스크래핑 완료!');
  console.log('='.repeat(80));
  console.log(`⏱️  총 소요 시간: ${totalTime}분`);
  console.log(`✅ 총 성공: ${totalSuccess}개`);
  console.log(`❌ 총 실패: ${totalFail}개`);
  console.log(`📄 결과 파일:`);
  workerResults.forEach(wr => {
    console.log(`   - kakao-scraping-results-worker-${wr.workerId}.json (${wr.successCount}개)`);
  });
  console.log('='.repeat(80) + '\n');
})();
