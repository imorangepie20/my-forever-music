# Recommendation Loop Policy

작성일: `2026-05-08`

## 방향

`PMS -> EMS -> GMS -> PMS` 흐름은 사용자가 모델 내부 값을 직접 편집하는 구조가 아니다.

- `PMS`: 사용자의 실제 플레이리스트, 승인된 저장곡, 재생 가능한 트랙 메타데이터, 피드백 이벤트를 저장한다.
- `EMS`: PMS 라이브러리와 외부 후보를 사용자 모델 관점에서 자동 평가한다.
- `GMS`: EMS를 통과한 후보를 사용자에게 최종 승인/거절/저장 액션으로 보여준다.
- `PMS`: GMS에서 저장된 곡과 피드백을 다시 사용자 모델 학습 재료로 보낸다.

## UI 원칙

- 사용자가 track id, artist name, genre 목록을 직접 편집하는 화면을 만들지 않는다.
- 추천 품질을 높이는 입력은 playlist import, playback, like/pass/save, Last.fm scrobble sync처럼 자연스러운 행동 이벤트로 수집한다.
- EMS/GMS 요청은 `user_id`와 `playlist_id` 중심으로 보내고, 수동 배열을 페이지에서 조립하지 않는다.
- 오류나 권한 문제는 타이머로 숨기지 않고 원인과 다음 조치를 명확히 드러낸다.

## 회귀 하네스

웹앱에는 `apps/web/scripts/product-flow-regression-harness.mjs`가 있다.

```bash
npm run test:product-flow
```

이 하네스는 PMS/EMS/GMS/Platforms/Home/Header/Sidebar 화면에 수동 seed 편집 UI나 seed payload 조립 코드가 다시 들어오면 실패한다.
