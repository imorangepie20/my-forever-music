# apps/web

`imapplepieTemplate001` 루트 Vite 템플릿을 기준으로 복사해온 프론트엔드 앱입니다.

## 포함된 것

- `src/` 전체 템플릿 구조
- `index.html`
- `package.json`
- `vite.config.ts`
- `tailwind.config.js`
- `postcss.config.js`
- `tsconfig.json`
- `tsconfig.node.json`

## 제외한 것

- `node_modules/`
- `dist/`
- `.git/`
- `ExercisePRJ/`
- `NaverRealEstateAnalyze/`
- 강의/참고 문서 폴더
- 잠금 파일과 tsbuildinfo

## 현재 상태

- 템플릿 페이지와 레이아웃은 유지됨
- `src/app`, `src/features`, `src/services`, `src/types` 같은 새 프로젝트용 확장 폴더도 유지됨
- 이후 `PMS / EMS / GMS` 중심으로 화면과 라우팅을 정리하면 됨
- 장기적으로 `apps/desktop`에서도 재사용할 수 있게 공통 로직 분리가 필요함
- 메인 백엔드는 `services/api`의 Spring Boot 계약을 기준으로 맞출 예정

## 다음 추천 작업

1. 템플릿 라우트를 새 제품 구조에 맞게 정리
2. 공통 디자인 토큰을 `packages/design-tokens`와 연결
3. API 호출 레이어를 `services/api`의 OpenAPI 계약에 맞게 재구성
4. 브라우저 전용 코드와 공통 코드를 분리
5. 인증, 홈, PMS, EMS, GMS 중심으로 우선 화면 축소
