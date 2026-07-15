# Re:Fail Frontend

Re:Fail의 Next.js 프론트엔드입니다. 실패 기록 탐색, 마크다운 작성·미리보기, 후속 기록, 내 기록 보관함, 관리자 화면을 제공합니다.

## 실행

```powershell
copy .env.example .env.local
npm install
npm run dev
```

기본 주소는 `http://localhost:3000`이며 백엔드 API는 `NEXT_PUBLIC_API_BASE_URL`로 설정합니다.

## 검증

```powershell
npm run lint
npm run build
```

전체 프로젝트 설명과 백엔드 실행 방법은 루트 [README](../README.md)를 참고합니다.
