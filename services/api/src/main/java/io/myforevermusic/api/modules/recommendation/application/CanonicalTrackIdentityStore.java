package io.myforevermusic.api.modules.recommendation.application;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface CanonicalTrackIdentityStore {

    String STATUS_ACTIVE = "active";

    UpsertResult upsertIdentity(Draft draft);

    Optional<IdentityEntry> findActiveIdentity(String source, String identityKind, String identityValue);

    List<IdentityEntry> findIdentitiesByCanonicalTrackId(Long canonicalTrackId);

    static String normalizeSource(String source) {
        return normalizeRequired(source, "source").toLowerCase(Locale.ROOT);
    }

    static String normalizeIdentityKind(String identityKind) {
        return normalizeRequired(identityKind, "identityKind").toLowerCase(Locale.ROOT);
    }

    static String normalizeIdentityValue(String identityKind, String identityValue) {
        String value = normalizeRequired(identityValue, "identityValue");
        String kind = normalizeIdentityKind(identityKind);
        if ("isrc".equals(kind)) {
            return value.toUpperCase(Locale.ROOT);
        }
        if ("mbid".equals(kind) || "musicbrainz_recording_id".equals(kind)) {
            return value.toLowerCase(Locale.ROOT);
        }
        if ("wikidata_qid".equals(kind)) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    record Draft(
        String displayTitle,
        String displayArtistName,
        String identityKind,
        String identityValue,
        String source,
        Double confidenceScore,
        Long createdFromCandidateId,
        Instant now
    ) {}

    record CanonicalTrackEntry(
        Long canonicalTrackId,
        String displayTitle,
        String displayArtistName,
        Instant createdAt,
        Instant updatedAt
    ) {}

    record IdentityEntry(
        Long canonicalTrackIdentityId,
        Long canonicalTrackId,
        String identityKind,
        String identityValue,
        String source,
        Double confidenceScore,
        String status,
        Long createdFromCandidateId,
        Instant createdAt,
        Instant updatedAt
    ) {}

    record UpsertResult(
        CanonicalTrackEntry canonicalTrack,
        IdentityEntry identity,
        boolean createdCanonicalTrack,
        boolean createdIdentity
    ) {}
}
