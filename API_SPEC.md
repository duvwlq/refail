# API 명세서

## 1. 문서 목적

이 문서는 `Fail` 프로젝트의 MVP 기준 API 명세를 정의한다.

기준 문서
- [PLAN.md](D:/Codex_Project/Fail/PLAN.md)
- [MVP_SPEC.md](D:/Codex_Project/Fail/MVP_SPEC.md)
- [HARNESS.md](D:/Codex_Project/Fail/HARNESS.md)
- [ERD.md](D:/Codex_Project/Fail/ERD.md)

현재 API 명세는 다음 기준을 따른다.
- 백엔드는 Spring Boot
- REST API 구조
- 댓글 기능은 MVP에서 제외
- 인증이 필요한 기능과 공개 기능을 명확히 구분
- 성공은 게시글이 아니라 업데이트 상태값으로 표현

## 2. 공통 규칙

### 기본 URL
`/api/v1`

### 응답 형식 원칙
성공 응답은 필요한 데이터만 명확히 반환한다.
에러 응답은 공통 구조를 사용한다.

### 공통 에러 응답 예시

```json
{
  "code": "POST_NOT_FOUND",
  "message": "게시글을 찾을 수 없습니다.",
  "timestamp": "2026-07-01T12:00:00"
}
```

### 인증 방식
- MVP 기준으로 세션 또는 JWT 중 하나를 선택 가능
- 이 문서에서는 `Authorization: Bearer <token>` 기준으로 서술

### 권한 구분
- `PUBLIC`: 비로그인 가능
- `USER`: 로그인 필요
- `ADMIN`: 관리자 권한 필요

### 소프트 삭제 원칙
- 삭제 API는 물리 삭제가 아니라 소프트 삭제를 우선한다

## 3. 공통 코드값

### 역할
- `USER`
- `ADMIN`

### 사용자 상태
- `ACTIVE`
- `RESTRICTED`
- `DELETED`

### 게시글 공개 방식
- `ANONYMOUS`
- `NICKNAME`

### 실패 크기
- `SMALL`
- `MEDIUM`
- `LARGE`

### 조언 선호
- `COMFORT`
- `ADVICE_OK`

### 업데이트 상태
- `STILL_FAILING`
- `TRYING_AGAIN`
- `IMPROVING`
- `SUCCEEDED`

### 공감 리액션 타입
- `ME_TOO`
- `SEND_SUPPORT`
- `THANKS_FOR_SHARING`
- `CHEERING_NEXT_TRY`

### 신고 사유
- `ABUSE`
- `HATE`
- `SPAM`
- `PRIVACY`
- `OTHER`

### 신고 처리 상태
- `PENDING`
- `RESOLVED`
- `REJECTED`

## 4. 인증 API

### 4-1. 회원가입

- Method: `POST`
- Path: `/api/v1/auth/signup`
- 권한: `PUBLIC`

요청 예시

```json
{
  "email": "user@example.com",
  "password": "password123!",
  "nickname": "실패기록러"
}
```

응답 예시

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "실패기록러",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-07-01T12:00:00"
}
```

검증 포인트
- 이메일 중복 불가
- 닉네임 중복 정책 적용
- 비밀번호 형식 검증

주요 에러
- `EMAIL_ALREADY_EXISTS`
- `NICKNAME_ALREADY_EXISTS`
- `INVALID_INPUT`

### 4-2. 로그인

- Method: `POST`
- Path: `/api/v1/auth/login`
- 권한: `PUBLIC`

요청 예시

```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

응답 예시

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "실패기록러",
    "role": "USER",
    "status": "ACTIVE"
  }
}
```

주요 에러
- `INVALID_CREDENTIALS`
- `USER_RESTRICTED`

### 4-3. 내 정보 조회

- Method: `GET`
- Path: `/api/v1/auth/me`
- 권한: `USER`

응답 예시

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "실패기록러",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-07-01T12:00:00"
}
```

## 5. 카테고리 API

### 5-1. 카테고리 목록 조회

- Method: `GET`
- Path: `/api/v1/categories`
- 권한: `PUBLIC`

응답 예시

```json
[
  {
    "categoryId": 1,
    "name": "공부",
    "slug": "study",
    "displayOrder": 1
  },
  {
    "categoryId": 2,
    "name": "다이어트",
    "slug": "diet",
    "displayOrder": 2
  }
]
```

## 6. 게시글 API

### 6-1. 게시글 작성

- Method: `POST`
- Path: `/api/v1/posts`
- 권한: `USER`

요청 예시

```json
{
  "categoryId": 1,
  "title": "오늘 또 계획만 세우고 공부를 못했다",
  "content": "해야 할 건 많았는데 결국 하루가 지나갔다.",
  "visibilityType": "ANONYMOUS",
  "failureSize": "SMALL",
  "emotionTag": "무기력",
  "advicePreference": "COMFORT",
  "retryIntention": true,
  "nextAttemptPlan": "다음에는 하루 목표를 한 가지로 줄여서 시작한다."
}
```

응답 예시

```json
{
  "postId": 10,
  "categoryId": 1,
  "title": "오늘 또 계획만 세우고 공부를 못했다",
  "visibilityType": "ANONYMOUS",
  "failureSize": "SMALL",
  "emotionTag": "무기력",
  "advicePreference": "COMFORT",
  "retryIntention": true,
  "nextAttemptPlan": "다음에는 하루 목표를 한 가지로 줄여서 시작한다.",
  "createdAt": "2026-07-01T12:10:00"
}
```

주요 에러
- `CATEGORY_NOT_FOUND`
- `INVALID_INPUT`
- `USER_RESTRICTED`

### 6-2. 게시글 목록 조회

- Method: `GET`
- Path: `/api/v1/posts`
- 권한: `PUBLIC`

쿼리 파라미터
- `sort`: `latest`, `popular`
- `categoryId`: 선택
- `page`: 기본 0
- `size`: 기본 20

요청 예시
- `/api/v1/posts?sort=latest&page=0&size=20`
- `/api/v1/posts?sort=popular&categoryId=2`

응답 예시

```json
{
  "content": [
    {
      "postId": 10,
      "category": {
        "categoryId": 1,
        "name": "공부"
      },
      "title": "오늘 또 계획만 세우고 공부를 못했다",
      "summary": "해야 할 건 많았는데 결국 하루가 지나갔다.",
      "visibilityType": "ANONYMOUS",
      "authorName": "익명",
      "failureSize": "SMALL",
      "emotionTag": "무기력",
      "reactionCount": 7,
      "hasUpdates": true,
      "createdAt": "2026-07-01T12:10:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

설명
- 익명 글이면 `authorName`은 `익명`
- 닉네임 글이면 `authorName`은 닉네임
- 목록에서는 본문 전체 대신 summary만 제공

### 6-3. 게시글 상세 조회

- Method: `GET`
- Path: `/api/v1/posts/{postId}`
- 권한: `PUBLIC`

응답 예시

```json
{
  "postId": 10,
  "category": {
    "categoryId": 1,
    "name": "공부"
  },
  "title": "오늘 또 계획만 세우고 공부를 못했다",
  "content": "해야 할 건 많았는데 결국 하루가 지나갔다.",
  "visibilityType": "ANONYMOUS",
  "authorName": "익명",
  "failureSize": "SMALL",
  "emotionTag": "무기력",
  "advicePreference": "COMFORT",
  "retryIntention": true,
  "nextAttemptPlan": "다음에는 하루 목표를 한 가지로 줄여서 시작한다.",
  "reactionSummary": {
    "totalCount": 7,
    "items": [
      {
        "reactionType": "ME_TOO",
        "count": 4
      },
      {
        "reactionType": "SEND_SUPPORT",
        "count": 3
      }
    ]
  },
  "updates": [
    {
      "updateId": 3,
      "status": "TRYING_AGAIN",
      "content": "이번 주부터 다시 계획을 줄여서 시작해보려 한다.",
      "createdAt": "2026-07-03T09:00:00"
    }
  ],
  "createdAt": "2026-07-01T12:10:00",
  "updatedAt": "2026-07-01T12:10:00"
}
```

주요 에러
- `POST_NOT_FOUND`
- `POST_HIDDEN`

### 6-3-1. 내 게시글 여부 조회

- Method: `GET`
- Path: `/api/v1/posts/{postId}/ownership`
- 권한: `USER`

```json
{
  "postId": 10,
  "ownedByMe": true
}
```

- 익명 작성자의 식별자를 공개하지 않고 현재 로그인 사용자의 소유 여부만 반환한다.
- 상세 화면의 수정·삭제 메뉴 노출에 사용한다.

### 6-4. 게시글 수정

- Method: `PATCH`
- Path: `/api/v1/posts/{postId}`
- 권한: `USER`

요청 예시

```json
{
  "categoryId": 2,
  "title": "제목 수정",
  "content": "내용 수정",
  "failureSize": "MEDIUM",
  "emotionTag": "답답함",
  "advicePreference": "ADVICE_OK",
  "retryIntention": false,
  "nextAttemptPlan": null
}
```

규칙
- 작성자만 수정 가능
- 공개 방식 변경 허용 여부는 정책에 따라 결정 가능
- `nextAttemptPlan`은 선택 입력이며 최대 500자

주요 에러
- `POST_NOT_FOUND`
- `FORBIDDEN`

### 6-5. 게시글 삭제

- Method: `DELETE`
- Path: `/api/v1/posts/{postId}`
- 권한: `USER`

응답 예시

```json
{
  "postId": 10,
  "deleted": true
}
```

규칙
- 작성자만 삭제 가능
- 소프트 삭제 처리

## 7. 업데이트 API

### 7-1. 업데이트 작성

- Method: `POST`
- Path: `/api/v1/posts/{postId}/updates`
- 권한: `USER`

요청 예시

```json
{
  "status": "IMPROVING",
  "content": "완벽하게 해결되진 않았지만 전보다 조금 나아졌다."
}
```

응답 예시

```json
{
  "updateId": 5,
  "postId": 10,
  "status": "IMPROVING",
  "content": "완벽하게 해결되진 않았지만 전보다 조금 나아졌다.",
  "createdAt": "2026-07-05T18:00:00"
}
```

규칙
- 원 작성자만 가능
- 하나의 글에 여러 업데이트 가능

주요 에러
- `POST_NOT_FOUND`
- `FORBIDDEN`
- `INVALID_INPUT`

### 7-2. 업데이트 목록 조회

- Method: `GET`
- Path: `/api/v1/posts/{postId}/updates`
- 권한: `PUBLIC`

응답 예시

```json
[
  {
    "updateId": 3,
    "status": "TRYING_AGAIN",
    "content": "이번 주부터 다시 계획을 줄여서 시작해보려 한다.",
    "createdAt": "2026-07-03T09:00:00"
  },
  {
    "updateId": 5,
    "status": "IMPROVING",
    "content": "완벽하게 해결되진 않았지만 전보다 조금 나아졌다.",
    "createdAt": "2026-07-05T18:00:00"
  }
]
```

## 8. 공감 리액션 API

### 8-0. 내 공감 상태 조회

- Method: `GET`
- Path: `/api/v1/posts/{postId}/reaction`
- 권한: `USER`

```json
{
  "postId": 10,
  "reactionType": "ME_TOO",
  "applied": true
}
```

- 공감하지 않은 상태이면 `reactionType`은 `null`, `applied`는 `false`다.
- 상세 화면 새로고침 후 선택한 공감을 복원하는 데 사용한다.

### 8-1. 공감 리액션 추가 또는 변경

- Method: `PUT`
- Path: `/api/v1/posts/{postId}/reaction`
- 권한: `USER`

요청 예시

```json
{
  "reactionType": "SEND_SUPPORT"
}
```

응답 예시

```json
{
  "postId": 10,
  "reactionType": "SEND_SUPPORT",
  "applied": true
}
```

규칙
- 같은 사용자는 한 게시글에 하나의 리액션만 가능
- 기존 리액션이 있으면 변경 처리

주요 에러
- `POST_NOT_FOUND`
- `INVALID_REACTION_TYPE`

### 8-2. 공감 리액션 취소

- Method: `DELETE`
- Path: `/api/v1/posts/{postId}/reaction`
- 권한: `USER`

응답 예시

```json
{
  "postId": 10,
  "removed": true
}
```

## 9. 신고 API

### 9-1. 게시글 신고

- Method: `POST`
- Path: `/api/v1/posts/{postId}/reports`
- 권한: `USER`

요청 예시

```json
{
  "reasonType": "ABUSE",
  "reasonDetail": "불필요하게 공격적인 표현이 포함되어 있습니다."
}
```

응답 예시

```json
{
  "reportId": 20,
  "postId": 10,
  "status": "PENDING",
  "createdAt": "2026-07-06T10:00:00"
}
```

규칙
- 본인 글 신고 허용 여부는 정책 선택 가능
- 동일 사용자의 중복 신고 제한 가능

주요 에러
- `POST_NOT_FOUND`
- `DUPLICATE_REPORT`

## 10. 관리자 API

### 10-1. 신고 목록 조회

- Method: `GET`
- Path: `/api/v1/admin/reports`
- 권한: `ADMIN`

쿼리 파라미터
- `status`: `PENDING`, `RESOLVED`, `REJECTED`
- `page`
- `size`

응답 예시

```json
{
  "content": [
    {
      "reportId": 20,
      "targetType": "POST",
      "targetId": 10,
      "reasonType": "ABUSE",
      "reasonDetail": "불필요하게 공격적인 표현이 포함되어 있습니다.",
      "status": "PENDING",
      "reporterUserId": 3,
      "createdAt": "2026-07-06T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 10-2. 게시글 숨김 처리

- Method: `PATCH`
- Path: `/api/v1/admin/posts/{postId}/hide`
- 권한: `ADMIN`

요청 예시

```json
{
  "reason": "운영 정책 위반"
}
```

응답 예시

```json
{
  "postId": 10,
  "hidden": true,
  "processedAt": "2026-07-06T10:10:00"
}
```

### 10-3. 게시글 숨김 해제

- Method: `PATCH`
- Path: `/api/v1/admin/posts/{postId}/unhide`
- 권한: `ADMIN`

응답 예시

```json
{
  "postId": 10,
  "hidden": false,
  "processedAt": "2026-07-06T10:20:00"
}
```

### 10-4. 사용자 제한

- Method: `PATCH`
- Path: `/api/v1/admin/users/{userId}/restrict`
- 권한: `ADMIN`

요청 예시

```json
{
  "reason": "반복 신고 누적"
}
```

응답 예시

```json
{
  "userId": 5,
  "status": "RESTRICTED",
  "processedAt": "2026-07-06T10:30:00"
}
```

### 10-5. 사용자 제한 해제

- Method: `PATCH`
- Path: `/api/v1/admin/users/{userId}/activate`
- 권한: `ADMIN`

응답 예시

```json
{
  "userId": 5,
  "status": "ACTIVE",
  "processedAt": "2026-07-06T10:40:00"
}
```

## 11. 에러 코드 초안

### 인증 관련
- `INVALID_CREDENTIALS`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `USER_RESTRICTED`

### 사용자 관련
- `USER_NOT_FOUND`
- `EMAIL_ALREADY_EXISTS`
- `NICKNAME_ALREADY_EXISTS`

### 게시글 관련
- `POST_NOT_FOUND`
- `POST_HIDDEN`
- `INVALID_POST_STATE`

### 카테고리 관련
- `CATEGORY_NOT_FOUND`

### 업데이트 관련
- `UPDATE_NOT_FOUND`

### 리액션 관련
- `INVALID_REACTION_TYPE`

### 신고 관련
- `DUPLICATE_REPORT`
- `REPORT_NOT_FOUND`

### 공통
- `INVALID_INPUT`
- `INTERNAL_SERVER_ERROR`

## 12. 우선 구현 순서

1. 인증 API
2. 카테고리 조회 API
3. 게시글 작성 / 목록 / 상세 API
4. 공감 리액션 API
5. 업데이트 API
6. 신고 API
7. 관리자 API

## 13. 이후 확장 포인트

### 댓글 기능이 생기면
- `POST /api/v1/posts/{postId}/comments`
- `GET /api/v1/posts/{postId}/comments`
- `PATCH /api/v1/comments/{commentId}`
- `DELETE /api/v1/comments/{commentId}`

### 이미지 첨부가 생기면
- 게시글 첨부 업로드 API
- 첨부 메타데이터 테이블

### 팔로우 기능이 생기면
- 사용자 관계 API
- 피드 재구성 API
