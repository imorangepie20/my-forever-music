package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.modules.platform.presentation.PlatformCatalogResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlatformCatalogService {

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
                "확보하지 못한 곡은 fallback 특성 생성 파이프라인으로 보강한다.",
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
                    "apple-music",
                    "Apple Music",
                    "planned-pms-import",
                    true,
                    true,
                    "cross-platform-spotify-match",
                    "사용자의 구독 플랫폼 플레이리스트를 PMS로 가져오는 주요 대상",
                    "공개 플레이리스트와 에디토리얼 흐름을 EMS 수집 대상으로 확장",
                    List.of(
                        "트랙 메타데이터를 Spotify 기준 특성과 매칭하는 보강 단계 필요",
                        "Spotify 미매칭 곡은 fallback 특성 생성으로 보완"
                    )
                ),
                new PlatformCatalogResponse.PlatformOption(
                    "tidal",
                    "TIDAL",
                    "planned-pms-import",
                    true,
                    true,
                    "cross-platform-spotify-match",
                    "사용자 고유 플레이리스트와 취향 신호를 PMS로 가져오는 대상",
                    "트렌딩 및 공개 플레이리스트를 EMS 후보군으로 수집하는 대상",
                    List.of(
                        "고음질 중심 사용자 취향과 niche catalog를 PMS/EMS 신호로 활용",
                        "Spotify 기준 특성과 매칭되지 않는 곡은 fallback 특성 생성 필요"
                    )
                )
            )
        );
    }
}
