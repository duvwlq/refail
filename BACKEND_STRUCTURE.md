# Spring Boot 프로젝트 구조

## 1. 목적

이 문서는 `Fail` 프로젝트의 Spring Boot 백엔드 구조를 빠르게 이해하기 위한 문서다.

현재 구조는 다음 기준으로 설계했다.
- MVP 범위만 반영
- 댓글 기능 제외
- 단일 애플리케이션 구조
- 도메인 중심 패키지 구성

## 2. 최상위 구조

```text
src
├─ main
│  ├─ java/com/fail/app
│  │  ├─ common
│  │  ├─ domain
│  │  └─ FailApplication.java
│  └─ resources
└─ test
```

## 3. 패키지 원칙

### common
공통으로 쓰는 설정, 예외, 응답, 베이스 엔티티를 둔다.

예시
- `common.config`
- `common.entity`
- `common.exception`
- `common.response`

### domain
기능별로 도메인을 나눈다.

현재 도메인
- `auth`
- `admin`
- `category`
- `moderation`
- `post`
- `reaction`
- `report`
- `user`

## 4. 도메인별 역할

### auth
- 회원가입
- 로그인
- 내 정보 조회

### user
- 사용자 엔티티
- 역할 / 상태 관리

### category
- 카테고리 관리
- 카테고리 조회

### post
- 실패 글
- 업데이트
- 게시글 조회/수정/삭제

### reaction
- 공감 리액션

### report
- 게시글 신고

### moderation
- 운영 이력

### admin
- 신고 목록 조회
- 게시글 숨김/복구
- 사용자 제한/해제

## 5. 현재 생성된 핵심 파일

### 공통
- `BaseTimeEntity`
- `SoftDeleteEntity`
- `JpaAuditingConfig`
- `ErrorCode`
- `ApiException`
- `GlobalExceptionHandler`
- `ErrorResponse`

### 도메인 엔티티
- `User`
- `Category`
- `Post`
- `PostUpdate`
- `Reaction`
- `Report`
- `ModerationAction`

### 기본 컨트롤러 자리
- `AuthController`
- `PostController`
- `AdminController`

## 6. 다음 구현 순서 추천

1. Repository 생성
2. Request/Response DTO 생성
3. Service 계층 생성
4. Auth API 구현
5. Post API 구현
6. Reaction / Update / Report 구현
7. Admin API 구현

## 7. 구조 선택 이유

- 도메인별로 파일이 모여 있어 기능 단위로 찾기 쉽다
- 단일 서버 구조라 과한 복잡성이 없다
- 이후 DTO, 서비스, 리포지토리를 같은 도메인 안에 확장하기 쉽다
- 포트폴리오에서 설계 의도를 설명하기 좋다
