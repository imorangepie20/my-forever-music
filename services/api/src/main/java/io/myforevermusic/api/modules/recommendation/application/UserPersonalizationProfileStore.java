package io.myforevermusic.api.modules.recommendation.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 사용자별 가벼운 개인화 프로필을 영속 저장한다.
 *
 * Why: Phase 5에서 전체 모델 재학습 없이 사용자별 rank를 바꾸려면, 최근 행동을 집계한 가벼운 신호가
 * 빠르게 조회되어야 한다. 본격적인 user embedding vector는 추후 SASRec head 학습 단계에서 갱신되며,
 * 이 store는 그 사이의 fast-path 신호(top artist, top source platform)를 제공한다.
 */
public interface UserPersonalizationProfileStore {

    Profile upsert(Draft draft);

    Optional<Profile> findByUserId(String userId);

    record Draft(
        String userId,
        List<ArtistAffinity> topArtists,
        List<PlatformAffinity> topSourcePlatforms,
        long eventCountAtUpdate,
        Instant lastEventAt,
        Instant recomputedAt
    ) {}

    record Profile(
        Long profileId,
        String userId,
        List<ArtistAffinity> topArtists,
        List<PlatformAffinity> topSourcePlatforms,
        long eventCountAtUpdate,
        Instant lastEventAt,
        Instant recomputedAt
    ) {}

    record ArtistAffinity(String artistName, double score, long signalCount) {}

    record PlatformAffinity(String platform, double score, long signalCount) {}
}
