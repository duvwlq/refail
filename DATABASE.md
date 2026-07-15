# MySQL 데이터베이스 실행 가이드

## 구성

- 운영 데이터베이스: MySQL 8.0
- 스키마 관리: Flyway
- JPA 스키마 정책: `ddl-auto=validate`
- 문자셋: `utf8mb4`
- 로컬 빠른 실행: H2 MySQL 호환 모드

애플리케이션 기본 프로필은 MySQL에 연결한다. 시작할 때 Flyway가 마이그레이션을 적용하고, 이후 Hibernate가 엔티티와 실제 스키마가 일치하는지 검증한다.

## 로컬 MySQL 실행

```powershell
docker compose up -d mysql
.\gradlew.bat bootRun --args="--spring.profiles.active=docker"
```

기본 접속 정보는 다음과 같다.

- Docker 주소: `localhost:13306`
- 직접 설치한 MySQL 기본 주소: `localhost:3306`
- 데이터베이스: `fail`
- 사용자: `fail`
- 비밀번호: `fail`

Docker 없이 백엔드 기능만 빠르게 확인할 때는 H2 기반 로컬 프로필을 사용한다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

## 환경 변수

배포 환경에서는 다음 값을 환경 변수로 주입한다.

```text
MYSQL_URL=jdbc:mysql://호스트:3306/fail?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul
MYSQL_USERNAME=서비스계정
MYSQL_PASSWORD=안전한비밀번호
JWT_SECRET=충분히긴랜덤문자열
```

저장소의 기본 비밀번호는 로컬 개발 전용이다. 운영 환경에서는 반드시 환경 변수로 교체한다.

## 마이그레이션 파일

- `V1__create_initial_schema.sql`: 사용자, 카테고리, 게시글, 후속 기록, 공감, 신고, 운영 이력 테이블 생성
- `V2__insert_default_categories.sql`: 공부, 다이어트, 일상 기본 카테고리 등록

이미 적용된 마이그레이션 파일은 수정하지 않는다. 스키마 변경이 필요하면 `V3__변경내용.sql`처럼 새 파일을 추가한다.

## 초기화 주의사항

`docker compose down`은 컨테이너만 중지하며 데이터 볼륨은 유지한다. 데이터 볼륨 삭제는 모든 로컬 데이터를 제거하므로 명시적으로 초기화할 때만 수행한다.
