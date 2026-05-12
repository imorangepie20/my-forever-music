package io.myforevermusic.api.modules.recommendation.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrackIdentityCandidateStore {

    String STATUS_PENDING = "pending";
    String STATUS_ACCEPTED = "accepted";
    String STATUS_REJECTED = "rejected";

    Entry save(Draft draft);

    Optional<Entry> findById(Long id);

    List<Entry> findRecentByStatus(String status, int limit);

    Entry updateStatus(Long id, String status, String resolvedBy, String notes, Instant resolvedAt);

    record Draft(
        String queryTitle,
        String queryArtist,
        String source,
        String candidateKind,
        String candidateValue,
        Double candidateScore,
        String metadata,
        String createdBy,
        Instant createdAt
    ) {}

    record Entry(
        Long id,
        String queryTitle,
        String queryArtist,
        String source,
        String candidateKind,
        String candidateValue,
        Double candidateScore,
        String metadata,
        String status,
        String createdBy,
        Instant createdAt,
        String resolvedBy,
        Instant resolvedAt,
        String notes
    ) {}
}
