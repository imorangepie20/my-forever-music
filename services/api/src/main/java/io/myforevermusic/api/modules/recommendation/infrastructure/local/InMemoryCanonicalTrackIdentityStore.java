package io.myforevermusic.api.modules.recommendation.infrastructure.local;

import io.myforevermusic.api.modules.recommendation.application.CanonicalTrackIdentityStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryCanonicalTrackIdentityStore implements CanonicalTrackIdentityStore {

    private final AtomicLong canonicalTrackSequence = new AtomicLong(1);
    private final AtomicLong identitySequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, CanonicalTrackEntry> tracks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, IdentityEntry> identities = new ConcurrentHashMap<>();

    @Override
    public UpsertResult upsertIdentity(Draft draft) {
        String source = CanonicalTrackIdentityStore.normalizeSource(draft.source());
        String kind = CanonicalTrackIdentityStore.normalizeIdentityKind(draft.identityKind());
        String value = CanonicalTrackIdentityStore.normalizeIdentityValue(kind, draft.identityValue());
        Optional<IdentityEntry> existing = findActiveIdentity(source, kind, value);
        if (existing.isPresent()) {
            CanonicalTrackEntry track = tracks.get(existing.get().canonicalTrackId());
            return new UpsertResult(track, existing.get(), false, false);
        }

        Instant now = draft.now() == null ? Instant.now() : draft.now();
        long canonicalTrackId = canonicalTrackSequence.getAndIncrement();
        CanonicalTrackEntry track = new CanonicalTrackEntry(
            canonicalTrackId,
            normalizeDisplayTitle(draft.displayTitle()),
            normalizeOptional(draft.displayArtistName()),
            now,
            now
        );
        tracks.put(canonicalTrackId, track);

        long identityId = identitySequence.getAndIncrement();
        IdentityEntry identity = new IdentityEntry(
            identityId,
            canonicalTrackId,
            kind,
            value,
            source,
            draft.confidenceScore(),
            STATUS_ACTIVE,
            draft.createdFromCandidateId(),
            now,
            now
        );
        identities.put(identityId, identity);
        return new UpsertResult(track, identity, true, true);
    }

    @Override
    public Optional<IdentityEntry> findActiveIdentity(String source, String identityKind, String identityValue) {
        String normalizedSource = CanonicalTrackIdentityStore.normalizeSource(source);
        String normalizedKind = CanonicalTrackIdentityStore.normalizeIdentityKind(identityKind);
        String normalizedValue = CanonicalTrackIdentityStore.normalizeIdentityValue(normalizedKind, identityValue);
        return identities.values().stream()
            .filter(identity -> STATUS_ACTIVE.equals(identity.status()))
            .filter(identity -> normalizedSource.equals(identity.source()))
            .filter(identity -> normalizedKind.equals(identity.identityKind()))
            .filter(identity -> normalizedValue.equals(identity.identityValue()))
            .findFirst();
    }

    @Override
    public List<IdentityEntry> findIdentitiesByCanonicalTrackId(Long canonicalTrackId) {
        return identities.values().stream()
            .filter(identity -> canonicalTrackId.equals(identity.canonicalTrackId()))
            .sorted(Comparator.comparing(IdentityEntry::createdAt).thenComparing(IdentityEntry::canonicalTrackIdentityId))
            .toList();
    }

    private String normalizeDisplayTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("displayTitle is required.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
