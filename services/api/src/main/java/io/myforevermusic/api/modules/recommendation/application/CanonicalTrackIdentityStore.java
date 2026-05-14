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

    /**
     * 기존 canonical track에 release_year/release_country/release_label이 비어 있으면 채운다.
     * 이미 채워진 값은 no-op. 새 값이 null이면 그 자리는 건드리지 않는다.
     * @return 갱신된 (또는 그대로인) canonical track entry
     */
    default CanonicalTrackEntry fillReleaseContextIfMissing(
        Long canonicalTrackId,
        String releaseYear,
        String releaseCountry,
        Instant now
    ) {
        return fillReleaseMetadataIfMissing(canonicalTrackId, releaseYear, releaseCountry, null, now);
    }

    CanonicalTrackEntry fillReleaseMetadataIfMissing(
        Long canonicalTrackId,
        String releaseYear,
        String releaseCountry,
        String releaseLabel,
        Instant now
    );

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
        String releaseYear,
        String releaseCountry,
        Instant now
    ) {}

    record CanonicalTrackEntry(
        Long canonicalTrackId,
        String displayTitle,
        String displayArtistName,
        String releaseYear,
        String releaseCountry,
        String releaseLabel,
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
