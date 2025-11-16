# Kotlin JDSL 도입 가이드

## 개요

Kotlin JDSL(Java Domain Specific Language)은 JPA Criteria API를 Kotlin DSL로 감싸서 타입 안전하고 읽기 쉬운 쿼리를 작성할 수 있게 해주는 라이브러리입니다.

### 주요 특징

- ✅ **코드 생성 불필요**: QueryDSL과 달리 Q-class 생성 과정이 없음
- ✅ **타입 안전**: 컴파일 타임에 오류 검출
- ✅ **가독성**: Kotlin DSL 문법으로 직관적인 쿼리 작성
- ✅ **동적 쿼리**: 조건에 따라 쿼리를 쉽게 조합 가능
- ✅ **Spring Data JPA 통합**: 기존 Repository와 자연스럽게 통합

### QueryDSL vs Kotlin JDSL

| 항목 | QueryDSL | Kotlin JDSL |
|------|----------|-------------|
| 코드 생성 | 필요 (Q-class) | 불필요 |
| 타입 안전성 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 가독성 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 빌드 속도 | 느림 (코드 생성) | 빠름 |
| Kotlin 친화성 | 보통 | ⭐⭐⭐⭐⭐ |

---

## 1. 의존성 추가

### build.gradle.kts

```kotlin
dependencies {
    // Kotlin JDSL 핵심 라이브러리
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.5.5")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.5.5")

    // Spring Data JPA 지원 모듈
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.5.5")

    // 기존 의존성들 (이미 있음)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // ...
}
```

**버전 참고**:
- 최신 버전은 [Maven Central](https://central.sonatype.com/artifact/com.linecorp.kotlin-jdsl/spring-data-jpa-support)에서 확인
- 2025년 1월 기준 3.5.5 안정 버전

---

## 2. Repository 설정

### 기본 Repository 확장

기존 JPA Repository에 `KotlinJdslJpqlExecutor`를 추가로 상속합니다.

```kotlin
// ReviewRepository.kt
package com.usktea.lunch.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.usktea.lunch.entity.ReviewEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<ReviewEntity, Long>,
                              KotlinJdslJpqlExecutor  // ← 이 인터페이스 추가
```

**변경 사항**:
- 기존 코드는 그대로 유지
- `KotlinJdslJpqlExecutor` 인터페이스만 추가로 상속
- 별도의 Configuration 클래스나 Bean 설정 불필요 (Spring Boot Auto Configuration이 자동 처리)

---

## 3. 기본 사용법

### 3.1 단순 조회 (WHERE 조건)

```kotlin
// Service에서 Repository 주입 (기존과 동일)
@Service
class ReviewEntityService(
    private val reviewRepository: ReviewRepository,
) {
    fun findByRestaurant(restaurantNumber: String): List<ReviewEntity> {
        return reviewRepository.findAll {
            select(
                entity(ReviewEntity::class)
            ).from(
                entity(ReviewEntity::class)
            ).where(
                path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
            )
        }
    }
}
```

**설명**:
- `findAll { }`: KotlinJdslJpqlExecutor가 제공하는 확장 함수
- `select()`: 조회할 엔티티 지정
- `from()`: 테이블(엔티티) 지정
- `where()`: 조건 지정
- `path(Entity::property)`: 엔티티의 프로퍼티 접근 (타입 안전)

### 3.2 복수 조건 (AND)

```kotlin
fun findByRestaurantAndRating(
    restaurantNumber: String,
    minRating: Int
): List<ReviewEntity> {
    return reviewRepository.findAll {
        select(entity(ReviewEntity::class))
            .from(entity(ReviewEntity::class))
            .where(
                and(
                    path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber),
                    path(ReviewEntity::rating).ge(minRating)  // greater than or equal
                )
            )
    }
}
```

**조건 연산자**:
- `eq()`: equals (=)
- `ne()`: not equals (!=)
- `lt()`: less than (<)
- `le()`: less than or equal (<=)
- `gt()`: greater than (>)
- `ge()`: greater than or equal (>=)
- `like()`: LIKE
- `isNull()`: IS NULL
- `isNotNull()`: IS NOT NULL

### 3.3 동적 쿼리 (조건부 WHERE)

```kotlin
fun searchReviews(
    restaurantNumber: String?,
    minRating: Int?,
    reviewerId: Long?
): List<ReviewEntity> {
    return reviewRepository.findAll {
        select(entity(ReviewEntity::class))
            .from(entity(ReviewEntity::class))
            .whereAnd(  // ← whereAnd는 null 조건을 자동으로 필터링
                restaurantNumber?.let {
                    path(ReviewEntity::restaurantManagementNumber).eq(it)
                },
                minRating?.let {
                    path(ReviewEntity::rating).ge(it)
                },
                reviewerId?.let {
                    path(ReviewEntity::reviewerId).eq(it)
                }
            )
    }
}
```

**핵심**:
- `whereAnd()`: null이 아닌 조건들만 AND로 결합
- `let { }`: 값이 null이 아닐 때만 조건 생성
- 이 패턴이 Kotlin JDSL의 가장 큰 장점 (동적 쿼리가 매우 간결함)

### 3.4 정렬 (ORDER BY)

```kotlin
fun findByRestaurantSorted(restaurantNumber: String): List<ReviewEntity> {
    return reviewRepository.findAll {
        select(entity(ReviewEntity::class))
            .from(entity(ReviewEntity::class))
            .where(
                path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
            )
            .orderBy(
                path(ReviewEntity::createdAt).desc(),  // 최신순
                path(ReviewEntity::id).asc()           // ID 오름차순
            )
    }
}
```

### 3.5 페이징 (LIMIT)

```kotlin
fun findTopReviews(restaurantNumber: String, limit: Int): List<ReviewEntity> {
    return reviewRepository.findAll {
        select(entity(ReviewEntity::class))
            .from(entity(ReviewEntity::class))
            .where(
                path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
            )
            .orderBy(path(ReviewEntity::rating).desc())
            .limit(limit)
    }
}
```

---

## 4. Cursor 기반 페이징 구현

귀하의 프로젝트에서 필요한 cursor 기반 페이징을 Kotlin JDSL로 구현하는 예제입니다.

```kotlin
// ReviewEntityService.kt
@Service
class ReviewEntityService(
    private val reviewRepository: ReviewRepository,
) {
    fun findReviewsWithCursor(
        restaurantManagementNumber: String,
        size: Int,
        sort: Sort,
        cursor: Long?
    ): CursorBasedPage<ReviewEntity> {
        val isDesc = sort.getOrderFor("id")?.isDescending ?: true

        // size + 1개 조회해서 hasNext 판단
        val results = reviewRepository.findAll {
            select(entity(ReviewEntity::class))
                .from(entity(ReviewEntity::class))
                .whereAnd(
                    // 필수 조건: 레스토랑 번호
                    path(ReviewEntity::restaurantManagementNumber)
                        .eq(restaurantManagementNumber),

                    // 선택 조건: cursor가 있으면 cursor 이후/이전 데이터만
                    cursor?.let {
                        if (isDesc) {
                            path(ReviewEntity::id).lt(it)  // 내림차순: id < cursor
                        } else {
                            path(ReviewEntity::id).gt(it)  // 오름차순: id > cursor
                        }
                    }
                )
                .orderBy(
                    if (isDesc) {
                        path(ReviewEntity::id).desc()
                    } else {
                        path(ReviewEntity::id).asc()
                    }
                )
                .limit(size + 1)  // hasNext 판단을 위해 +1
        }

        val content = results.take(size)
        val hasNext = results.size > size

        return pageOf(content, hasNext) { it.id.toString() }
    }
}
```

**장점**:
1. cursor가 null일 때와 있을 때의 쿼리 분기를 자연스럽게 처리
2. 정렬 방향(ASC/DESC)에 따른 조건 변경이 명확함
3. 타입 안전: `id`가 Long이 아니면 컴파일 에러

---

## 5. 고급 기능

### 5.1 집계 쿼리 (COUNT, AVG)

```kotlin
fun getAverageRating(restaurantNumber: String): Double? {
    return reviewRepository.findAll {
        select(
            avg(path(ReviewEntity::rating))  // AVG 집계
        ).from(
            entity(ReviewEntity::class)
        ).where(
            path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
        )
    }.firstOrNull()
}

fun countReviews(restaurantNumber: String): Long {
    return reviewRepository.findAll {
        select(
            count(entity(ReviewEntity::class))
        ).from(
            entity(ReviewEntity::class)
        ).where(
            path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
        )
    }.first()
}
```

### 5.2 서브쿼리

```kotlin
fun findHighRatedRestaurants(minAvgRating: Double): List<ReviewEntity> {
    return reviewRepository.findAll {
        val subquery = select(
            path(ReviewEntity::restaurantManagementNumber)
        ).from(
            entity(ReviewEntity::class)
        ).groupBy(
            path(ReviewEntity::restaurantManagementNumber)
        ).having(
            avg(path(ReviewEntity::rating)).ge(minAvgRating)
        ).asSubquery()

        select(entity(ReviewEntity::class))
            .from(entity(ReviewEntity::class))
            .where(
                path(ReviewEntity::restaurantManagementNumber).`in`(subquery)
            )
    }
}
```

### 5.3 JOIN (연관 관계가 있는 경우)

```kotlin
// 예시: Review와 User가 @ManyToOne 관계라고 가정
fun findReviewsWithUser(restaurantNumber: String): List<ReviewEntity> {
    return reviewRepository.findAll {
        select(entity(ReviewEntity::class))
            .from(
                entity(ReviewEntity::class),
                join(ReviewEntity::user)  // user 프로퍼티로 조인
            )
            .where(
                path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
            )
    }
}
```

---

## 6. 제한 사항 및 주의사항

### 6.1 PostgreSQL 특화 기능 (PostGIS)

Kotlin JDSL은 표준 JPA/JPQL만 지원하므로, PostGIS 공간 함수는 사용할 수 없습니다.

```kotlin
// ❌ 불가능: PostGIS ST_DWithin 함수
// Kotlin JDSL로는 표현 불가

// ✅ 대안: Native Query 사용
@Query(value = """
    SELECT * FROM lunch.restaurant r
    WHERE ST_DWithin(r.location::geography,
                     ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                     :radius)
""", nativeQuery = true)
fun findNearby(
    @Param("lat") lat: Double,
    @Param("lng") lng: Double,
    @Param("radius") radius: Double
): List<RestaurantEntity>
```

**권장 사항**:
- 일반 쿼리: Kotlin JDSL 사용
- 공간 쿼리: Native Query 사용

### 6.2 성능 고려사항

```kotlin
// ❌ N+1 문제 발생 가능
fun findReviewsWithImages(restaurantNumber: String): List<ReviewEntity> {
    return reviewRepository.findAll {
        select(entity(ReviewEntity::class))
            .from(entity(ReviewEntity::class))
            .where(
                path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
            )
    }
    // imageUrls가 LAZY 로딩이면 각 엔티티마다 추가 쿼리 발생
}

// ✅ JOIN FETCH로 해결
fun findReviewsWithImages(restaurantNumber: String): List<ReviewEntity> {
    return reviewRepository.findAll {
        select(entity(ReviewEntity::class))
            .from(
                entity(ReviewEntity::class),
                fetch(ReviewEntity::imageUrls)  // FETCH JOIN
            )
            .where(
                path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
            )
    }
}
```

### 6.3 RenderContext 재사용

Kotlin JDSL은 내부적으로 `RenderContext`를 사용하는데, 이 객체 생성 비용이 비쌉니다.

**Spring Data JPA Support 모듈 사용 시**: 자동으로 최적화됨 (걱정 불필요)

---

## 7. 마이그레이션 전략

### 기존 코드 유지하면서 점진적 도입

```kotlin
interface ReviewRepository : JpaRepository<ReviewEntity, Long>,
                              KotlinJdslJpqlExecutor {

    // 기존 메서드는 그대로 유지
    fun findByRestaurantManagementNumber(number: String): List<ReviewEntity>

    @Query("SELECT r FROM ReviewEntity r WHERE r.rating >= :minRating")
    fun findHighRated(@Param("minRating") minRating: Int): List<ReviewEntity>

    // 새로운 기능은 Service에서 Kotlin JDSL로 구현
}

@Service
class ReviewEntityService(
    private val reviewRepository: ReviewRepository,
) {
    // 기존 메서드 호출
    fun findByRestaurant(number: String) =
        reviewRepository.findByRestaurantManagementNumber(number)

    // 새로운 동적 쿼리는 Kotlin JDSL 사용
    fun searchWithCursor(...) = reviewRepository.findAll { ... }
}
```

**권장 순서**:
1. 의존성 추가 및 Repository 인터페이스 확장
2. 새로운 기능(cursor 페이징 등)부터 Kotlin JDSL로 구현
3. 기존 쿼리는 점진적으로 마이그레이션 (또는 유지)

---

## 8. 실전 예제: ReviewEntityService 완성

```kotlin
package com.usktea.lunch.service.entity

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.usktea.lunch.common.CursorBasedPage
import com.usktea.lunch.common.pageOf
import com.usktea.lunch.entity.ReviewEntity
import com.usktea.lunch.repository.ReviewRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewEntityService(
    private val reviewRepository: ReviewRepository,
) {
    @Transactional
    fun save(review: ReviewEntity): ReviewEntity {
        return reviewRepository.save(review)
    }

    fun findReviewsWithCursor(
        restaurantManagementNumber: String,
        size: Int,
        sort: Sort,
        cursor: Long?
    ): CursorBasedPage<ReviewEntity> {
        val isDesc = sort.getOrderFor("id")?.isDescending ?: true

        val results = reviewRepository.findAll {
            select(entity(ReviewEntity::class))
                .from(entity(ReviewEntity::class))
                .whereAnd(
                    path(ReviewEntity::restaurantManagementNumber)
                        .eq(restaurantManagementNumber),
                    cursor?.let {
                        if (isDesc) {
                            path(ReviewEntity::id).lt(it)
                        } else {
                            path(ReviewEntity::id).gt(it)
                        }
                    }
                )
                .orderBy(
                    if (isDesc) {
                        path(ReviewEntity::id).desc()
                    } else {
                        path(ReviewEntity::id).asc()
                    }
                )
                .limit(size + 1)
        }

        val content = results.take(size)
        val hasNext = results.size > size

        return pageOf(content, hasNext) { it.id.toString() }
    }

    // 추가 예제: 평점별 리뷰 검색
    fun findByRatingRange(
        restaurantNumber: String,
        minRating: Int?,
        maxRating: Int?
    ): List<ReviewEntity> {
        return reviewRepository.findAll {
            select(entity(ReviewEntity::class))
                .from(entity(ReviewEntity::class))
                .whereAnd(
                    path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber),
                    minRating?.let { path(ReviewEntity::rating).ge(it) },
                    maxRating?.let { path(ReviewEntity::rating).le(it) }
                )
                .orderBy(path(ReviewEntity::createdAt).desc())
        }
    }

    // 추가 예제: 평균 평점 계산
    fun getAverageRating(restaurantNumber: String): Double {
        return reviewRepository.findAll {
            select(avg(path(ReviewEntity::rating)))
                .from(entity(ReviewEntity::class))
                .where(
                    path(ReviewEntity::restaurantManagementNumber).eq(restaurantNumber)
                )
        }.firstOrNull() ?: 0.0
    }
}
```

---

## 9. 참고 자료

- [Kotlin JDSL 공식 문서](https://kotlin-jdsl.gitbook.io/docs/)
- [GitHub Repository](https://github.com/line/kotlin-jdsl)
- [Maven Central](https://central.sonatype.com/artifact/com.linecorp.kotlin-jdsl/spring-data-jpa-support)

---

## 10. FAQ

### Q1. QueryDSL에서 마이그레이션할 수 있나요?
**A**: 가능합니다. 문법이 유사하여 학습 곡선이 낮습니다.

### Q2. 성능 차이가 있나요?
**A**: 최종적으로 JPA Criteria API로 변환되므로 성능은 동일합니다.

### Q3. 기존 @Query 메서드를 모두 바꿔야 하나요?
**A**: 아니요. 공존 가능하며 점진적으로 마이그레이션할 수 있습니다.

### Q4. Spring Boot 3.x와 호환되나요?
**A**: 네, 완벽히 호환됩니다. (귀하의 프로젝트는 3.3.5 사용 중)

### Q5. Kotlin JDSL이 적합하지 않은 경우는?
**A**:
- 매우 복잡한 Native SQL이 필요한 경우 (PostGIS 등)
- 팀 전체가 Kotlin에 익숙하지 않은 경우
- 단순 CRUD만 필요한 경우 (Spring Data JPA 메서드 이름 규칙으로 충분)