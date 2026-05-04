package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.modules.platform.presentation.PlatformCatalogResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlatformCatalogService {

    public PlatformCatalogResponse.PlatformOption getRequiredPlatform(String platformId) {
        return getCatalog().platforms().stream()
            .filter(platform -> platform.platformId().equals(platformId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Platform is not supported: %s".formatted(platformId)));
    }

    public PlatformCatalogResponse getCatalog() {
        return new PlatformCatalogResponse(
            "api",
            "ok",
            Instant.now(),
            "spotify",
            List.of(
                "사용자가 구독 중인 스트리밍 플랫폼을 선택한다.",
                "선택한 플랫폼의 플레이리스트를 PMS로 가져온다.",
                "트랙별 오디오 특성을 우선 Spotify 기준으로 확보한다.",
                "Spotify 오디오 특성을 확보하지 못한 곡은 가짜 특성으로 채우지 않고 import를 중단한다.",
                "스트리밍 플랫폼 확장은 Spotify -> TIDAL -> YouTube Music 순서로 진행하고 Apple Music은 개발자 계정 확보 후 재개한다.",
                "가져온 데이터는 EMS/GMS 추천 루프와 사용자 모델 학습에 연결한다."
            ),
            List.of(
                new PlatformCatalogResponse.PlatformOption(
                    "spotify",
                    "Spotify",
                    "priority-analysis-source",
                    true,
                    true,
                    "native-audio-features",
                    "사용자 플레이리스트 PMS 적재의 1차 기준 플랫폼",
                    "공개 플레이리스트와 트렌드 수집의 우선 연구 대상",
                    List.of(
                        "핵심 오디오 특성 기준 소스",
                        "danceability, energy, valence, acousticness, liveness, speechiness, tempo를 우선 사용",
                        "개인화 모델의 공통 기반 데이터셋 구축에 가장 적합한 시작점"
                    )
                ),
                new PlatformCatalogResponse.PlatformOption(
                    "tidal",
                    "TIDAL",
                    "planned-provider-next",
                    false,
                    true,
                    "disabled-until-real-provider",
                    "Spotify 다음으로 실제 TIDAL provider 구현을 진행하며, 완료 전까지 PMS import 비활성",
                    "트렌딩 및 공개 플레이리스트를 EMS 후보군으로 수집하는 다음 대상",
                    List.of(
                        "현재 사용자 온보딩에서는 선택할 수 없다",
                        "TIDAL OAuth 2.1 + PKCE 토큰 교환 기반을 먼저 준비한다",
                        "실제 TIDAL API playlist provider와 Spotify 오디오 특성 매칭 검증이 끝난 뒤 PMS import를 활성화한다"
                    )
                ),
                new PlatformCatalogResponse.PlatformOption(
                    "youtube-music",
                    "YouTube Music",
                    "planned-provider-after-tidal",
                    false,
                    true,
                    "disabled-until-real-provider",
                    "TIDAL provider 안정화 후 실제 YouTube Music provider 구현 예정",
                    "알고리즘 믹스와 재생 흐름을 EMS 수집 신호로 활용할 수 있는 대상",
                    List.of(
                        "현재 사용자 온보딩에서는 선택할 수 없다",
                        "TIDAL 연동과 PMS import가 안정화된 뒤 구현한다",
                        "트랙 메타데이터와 Spotify 특성 매칭이 실제 API로 검증될 때까지 사용자 플로우에 노출하지 않는다"
                    )
                ),
                new PlatformCatalogResponse.PlatformOption(
                    "apple-music",
                    "Apple Music",
                    "deferred-developer-account",
                    false,
                    true,
                    "disabled-until-real-provider",
                    "Apple Developer 계정 준비 전까지 실제 Apple Music provider 구현 보류",
                    "공개 플레이리스트와 에디토리얼 흐름을 EMS 수집 대상으로 확장할 후보",
                    List.of(
                        "현재 사용자 온보딩에서는 선택할 수 없다",
                        "개발자 계정과 MusicKit/API 권한이 준비된 뒤 다시 진행한다",
                        "실제 Apple Music API 연동과 Spotify 특성 매칭 검증이 끝날 때까지 사용자 플로우에 노출하지 않는다"
                    )
                ),
                new PlatformCatalogResponse.PlatformOption(
                    "last-fm",
                    "Last.fm",
                    "analysis-signal-source",
                    false,
                    true,
                    "scrobble-history-signal",
                    "플레이리스트 import보다는 장기 청취 이력과 아티스트 affinity를 보강하는 신호 소스",
                    "scrobble, top track, top artist 데이터를 EMS/GMS 학습 신호로 연결하는 대상",
                    List.of(
                        "현재 단계에서는 PMS playlist import 대상이 아니다",
                        "public profile preview로 recent scrobble, top artist, top track 신호를 먼저 읽는다",
                        "재생 이력과 태그, top artist 데이터를 장기 취향 모델 입력으로 사용할 계획이다"
                    )
                )
            )
        );
    }
}
