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

### MySQL FULLTEXT 검색

Flyway `V7`에서 `posts(title, content, emotion_tag)`에 ngram 파서를 사용하는 FULLTEXT 인덱스를 추가했다.

- 2글자 이상 검색어는 MySQL FULLTEXT로 ID 페이지를 먼저 조회한다.
- 조회된 ID의 게시글·작성자·카테고리는 `EntityGraph`로 한 번에 조회한다.
- 1글자 검색과 H2 기반 테스트는 기존 LIKE 검색으로 처리한다.
- 숨김·삭제, 카테고리, 실패 크기, 최신·공감순 정책은 기존과 동일하게 유지한다.
- `SEARCH_FULLTEXT_ENABLED=false`로 FULLTEXT 경로를 끌 수 있다.

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
SHOW INDEX FROM posts WHERE Key_name = 'idx_posts_fulltext_search';
```

## 검색 벤치마크

2026-07-24 로컬 Docker Desktop의 MySQL 8.4에서 동일 데이터 생성 규칙으로 LIKE와 FULLTEXT를 비교했다. 각 조건은 3회 워밍업 후 10회 측정했다.

### 게시글 1만 건

| 시나리오 | LIKE 평균/p95 | FULLTEXT 평균/p95 | 결과 수 |
| --- | ---: | ---: | ---: |
| 제목 `다이어트` | 16.44 / 21.11ms | 3.91 / 5.00ms | 1,000 |
| 본문 `알고리즘` | 16.18 / 19.67ms | 2.47 / 2.79ms | 400 |
| 카테고리 + `면접` | 16.73 / 35.83ms | 1.76 / 1.98ms | 500 |

### 게시글 5만 건

| 시나리오 | LIKE 평균/p95 | FULLTEXT 평균/p95 | 결과 수 |
| --- | ---: | ---: | ---: |
| 제목 `다이어트` | 71.99 / 126.58ms | 15.94 / 20.86ms | 5,000 |
| 본문 `알고리즘` | 72.47 / 105.49ms | 6.57 / 7.84ms | 2,000 |
| 카테고리 + `면접` | 62.41 / 93.65ms | 5.27 / 5.72ms | 2,500 |

LIKE 실행 계획은 노출 가능한 게시글 5만 건을 스캔했다. FULLTEXT는 `idx_posts_fulltext_search`의 전문 검색 결과만 읽었다. 세 시나리오 모두 결과 건수가 같아 현재 한국어 예시 검색의 정확도도 유지됐다.

재현 명령:

```powershell
.\scripts\benchmark-search.ps1 -PostCount 10000
.\scripts\benchmark-search.ps1 -PostCount 50000
```

보고서는 `build/reports/search-benchmark-{건수}.md`에 생성되며 `EXPLAIN ANALYZE` 결과를 포함한다.

### 비용과 한계

- FULLTEXT 인덱스를 유지하면서 게시글 5만 건을 단일 테스트 컨테이너에 순차 적재하는 데 약 11분이 걸렸다.
- 이 수치는 실제 서비스의 단건 작성 지연이 아니라 벤치마크 데이터 생성 전체 시간이다.
- ngram 인덱스는 일반 B-Tree보다 저장 공간과 쓰기 비용이 크다.
- 한 글자 검색은 ngram 특성상 LIKE로 폴백한다.
- 동의어, 오탈자, 형태소 기반 검색이 필요해질 때 별도 검색 엔진을 검토한다.

## 남은 확장 지점

현재 데이터 규모에서는 MySQL FULLTEXT가 충분한 개선을 제공하므로 별도 검색 서버는 도입하지 않는다. 검색 품질 요구가 전문 검색 수준으로 커질 때만 전환한다.
