package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsTrackAudioFeatures;
import io.myforevermusic.api.modules.gms.presentation.GmsRecommendationPreviewResponse.RecommendationItem;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore.LibraryPlaylistState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * GMS 추천이 cold-start 사용자에 대해 어쩔 수 없이 빈 응답을 반환하던 흐름에서, EMS 본 테이블의
 * 최근 audio feature 가 채워진 트랙을 fallback 후보로 제공해 사용자가 추천을 처음 사용해도
 * 무언가 볼 수 있게 한다.
 *
 * Why (Phase 5 sub-item 4): 사용자가 PMS user_library 를 아직 import 하지 않은 상태("cold-start")
 * 에서는 personalization 신호가 없고 PMS 기반 mapping 도 불가능해서, 기존 GMS preview 가
 * "Import a real playlist..." 오류로 끊겼다. 이 fallback 은 같은 상황에서 EMS pool 의 최근
 * 트랙을 보여줌으로써 "무엇을 들을 수 있는지" 감을 잡게 한다. 사용자에게는 어떤 곡이라도 보이는 것이
 * 빈 화면보다 낫다.
 *
 * 사용자 식별: AuthAccountStore.preferredPlatformId 가 있으면 그 플랫폼 우선, 없으면
 * findDistinctSourcePlatforms 의 첫 항목 사용. 두 경우 모두 audio_features_filled 가 true 인
 * 트랙을 우선 정렬한다.
 */
@Service
public class ColdStartFallbackService {

    private final AuthAccountStore authAccountStore;
    private final PmsUserLibraryStore pmsUserLibraryStore;
    private final Optional<EmsCollectedTrackRepository> emsTrackRepository;

    @Value("${app.recommendation.cold-start.fallback-limit:12}")
    private int fallbackLimit;

    public ColdStartFallbackService(
        AuthAccountStore authAccountStore,
        PmsUserLibraryStore pmsUserLibraryStore,
        Optional<EmsCollectedTrackRepository> emsTrackRepository
    ) {
        this.authAccountStore = authAccountStore;
        this.pmsUserLibraryStore = pmsUserLibraryStore;
        this.emsTrackRepository = emsTrackRepository;
    }

    /**
     * @return true if the user has no PMS library tracks at all.
     */
    public boolean isColdStart(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        List<LibraryPlaylistState> playlists = pmsUserLibraryStore.findPlaylists(userId);
        long trackCount = 0L;
        for (LibraryPlaylistState playlist : playlists) {
            if (playlist.tracks() != null) {
                trackCount += playlist.tracks().size();
                if (trackCount > 0L) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return EMS-backed fallback items for a cold-start user (empty when EMS pool is also empty).
     */
    public List<RecommendationItem> fallbackItems(String userId, Integer limitOverride) {
        if (emsTrackRepository.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(50, limitOverride == null ? fallbackLimit : limitOverride));
        String preferredPlatform = resolvePreferredPlatform(userId);
        List<EmsCollectedTrackEntity> source = selectSourceTracks(preferredPlatform);
        if (source.isEmpty()) {
            return List.of();
        }
        return source.stream()
            .sorted(Comparator
                .comparing((EmsCollectedTrackEntity track) -> isAudioFilled(track) ? 0 : 1)
                .thenComparing((EmsCollectedTrackEntity track) -> track.getCollectedAt(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
            )
            .limit(safeLimit)
            .map(this::toRecommendationItem)
            .toList();
    }

    private String resolvePreferredPlatform(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return authAccountStore.findByUserId(userId)
            .map(account -> account.preferredPlatformId())
            .filter(value -> value != null && !value.isBlank())
            .orElse(null);
    }

    private List<EmsCollectedTrackEntity> selectSourceTracks(String preferredPlatform) {
        EmsCollectedTrackRepository repository = emsTrackRepository.get();
        if (preferredPlatform != null) {
            List<EmsCollectedTrackEntity> tracks = repository.findBySourcePlatformOrderByCollectedAtDesc(preferredPlatform);
            if (!tracks.isEmpty()) {
                return tracks;
            }
        }
        // Preferred platform empty; pick the first available platform that has tracks.
        List<EmsCollectedTrackEntity> aggregated = new ArrayList<>();
        for (String platform : repository.findDistinctSourcePlatforms()) {
            if (platform == null || platform.equals(preferredPlatform)) {
                continue;
            }
            List<EmsCollectedTrackEntity> tracks = repository.findBySourcePlatformOrderByCollectedAtDesc(platform);
            if (!tracks.isEmpty()) {
                aggregated.addAll(tracks);
                break;
            }
        }
        return aggregated;
    }

    private boolean isAudioFilled(EmsCollectedTrackEntity track) {
        EmsTrackAudioFeatures features = track.getAudioFeatures();
        return features != null && features.isAudioFeaturesFilled();
    }

    private RecommendationItem toRecommendationItem(EmsCollectedTrackEntity track) {
        return new RecommendationItem(
            null,
            "ems-%d".formatted(track.getId()),
            track.getTitle(),
            track.getArtistName(),
            track.getSourcePlatform(),
            null,
            null,
            track.getAlbumTitle(),
            track.getAlbumImageUrl(),
            track.getPlatformExternalUrl(),
            track.getSpotifyUri(),
            track.getPreviewUrl(),
            "spotify".equalsIgnoreCase(track.getSourcePlatform()) ? track.getExternalTrackId() : null,
            track.getDurationMs(),
            0.0d,
            "cold_start",
            null,
            "Cold-start fallback from EMS pool"
        );
    }
}
