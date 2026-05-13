package io.myforevermusic.api.modules.recommendation.application;

import java.time.Instant;
import java.util.List;

public interface TrackIdentityCandidateAuditStore {

    String ACTION_APPLY = "apply";
    String ACTION_ROLLBACK = "rollback";
    String ACTION_NO_MATCH = "no_match";
    String ACTION_CONFLICT = "conflict";
    String ACTION_REVIEW_REQUIRED = "review_required";
    String ACTION_CANONICAL_PROMOTE = "canonical_promote";

    String STATUS_APPLIED = "applied";
    String STATUS_ALREADY_MATCHED = "already_matched";
    String STATUS_CONFLICT = "conflict";
    String STATUS_NO_MATCH = "no_match";
    String STATUS_ROLLED_BACK = "rolled_back";
    String STATUS_REVIEW_REQUIRED = "review_required";
    String STATUS_CANONICAL_PROMOTED = "canonical_promoted";
    String STATUS_CANONICAL_EXISTS = "canonical_exists";

    Entry save(Draft draft);

    List<Entry> findByCandidateId(Long candidateId);

    List<Entry> findByCandidateIdAndAction(Long candidateId, String action);

    record Draft(
        Long candidateId,
        String action,
        Long emsCollectedTrackId,
        String candidateValue,
        String previousIsrc,
        String newIsrc,
        String status,
        String message,
        String actedBy,
        Instant actedAt
    ) {}

    record Entry(
        Long id,
        Long candidateId,
        String action,
        Long emsCollectedTrackId,
        String candidateValue,
        String previousIsrc,
        String newIsrc,
        String status,
        String message,
        String actedBy,
        Instant actedAt
    ) {}
}
