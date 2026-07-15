# 백엔드 성능 설계

## 목표

Re:Fail의 주요 읽기 API가 게시글 수 증가에 따라 불필요하게 느려지지 않도록 조회 쿼리 수와 인덱스를 관리한다. 최적화는 현재의 Spring Boot 단일 서버 구조를 유지하면서 적용한다.

## 적용한 최적화

### 게시글 목록과 상세 N+1 제거

- 게시글 목록에서 `Post.category`, `Post.user`를 `EntityGraph`로 함께 조회한다.
- 게시글 상세도 작성자와 카테고리를 한 쿼리로 조회한다.
- 후속 기록 존재 여부는 게시글마다 조회하지 않고 현재 페이지의 게시글 ID를 한 번에 조회한다.
- 목록 SQL은 게시글 수와 무관하게 최대 3회다: 목록 조회, 전체 개수 조회, 후속 기록 존재 여부 조회.

### 관리자 신고 목록 N+1 제거

- 신고 목록과 신고자 정보를 한 번에 조회한다.
- 신고 건수가 늘어나도 신고자 조회 SQL이 건별로 추가되지 않는다.

### 카테고리 캐시

- 활성 카테고리 목록과 검색 결과를 애플리케이션 메모리에 캐시한다.
- 관리자가 카테고리를 생성, 수정, 비활성화하면 관련 캐시를 모두 제거한다.
- 단일 서버 포트폴리오 운영에는 로컬 캐시가 적합하다. 서버를 여러 대로 확장하면 Redis 같은 공유 캐시로 교체해야 한다.

### MySQL 인덱스

Flyway `V4`에서 다음 복합 인덱스를 추가한다.

- `posts(user_id, deleted_at, created_at)`: 내 기록 보관함
- `posts(failure_size, hidden, deleted_at, created_at)`: 실패 크기 필터와 최신순
- `post_updates(status, deleted_at)`: 운영 지표의 후속 기록 상태 집계

인덱스는 읽기를 빠르게 하지만 쓰기 비용과 저장 공간을 늘린다. 실제 API 조회 조건에 쓰이는 인덱스만 추가한다.

### JPA와 로그 설정

- `hibernate.default_batch_fetch_size=100`으로 남아 있는 지연 로딩을 묶어서 처리한다.
- JDBC 쓰기 배치 크기를 50으로 설정하고 INSERT/UPDATE 순서를 정렬한다.
- 운영 기본값에서 Hibernate SQL 디버그 로그를 끈다. 필요할 때만 `HIBERNATE_SQL_LOG_LEVEL=debug`로 활성화한다.

## 검증

`PostQueryPerformanceIntegrationTest`는 게시글 10개를 조회해도 준비된 SQL이 3회를 넘지 않는지 검사한다.

```powershell
.\gradlew.bat test
```

2026-07-15 로컬 환경에서 워밍업 후 각 API를 5회 호출한 참고 결과는 다음과 같다. 데이터가 적은 개발 환경의 수치이므로 절대 성능 목표가 아니라 변경 전후 비교 기준으로 사용한다.

| API | 평균 | 최소 | 최대 |
| --- | ---: | ---: | ---: |
| `GET /api/v1/posts?size=20` | 26.29ms | 21.73ms | 35.59ms |
| `GET /api/v1/categories` | 12.38ms | 11.90ms | 12.83ms |
| `GET /api/v1/health` | 13.08ms | 12.55ms | 13.66ms |

MySQL 적용 인덱스는 다음 명령으로 확인할 수 있다.

```sql
SHOW INDEX FROM posts;
SHOW INDEX FROM post_updates;
```

## 남은 확장 지점

현재 제목, 본문, 감정 태그 검색은 `%키워드%` 조건이므로 일반 B-Tree 인덱스를 충분히 활용하지 못한다. 게시글이 수만 건 이상으로 늘고 실제 검색 지연이 확인되면 MySQL FULLTEXT 인덱스와 전문 검색 쿼리를 도입한다. 포트폴리오 규모에서 미리 검색 서버를 추가하는 것은 운영 복잡도와 비용이 더 크므로 보류한다.
