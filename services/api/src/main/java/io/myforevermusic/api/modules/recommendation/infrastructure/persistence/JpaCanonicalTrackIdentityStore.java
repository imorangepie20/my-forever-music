package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.CanonicalTrackIdentityStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaCanonicalTrackIdentityStore implements CanonicalTrackIdentityStore {

    private final CanonicalTrackRepository trackRepository;
    private final CanonicalTrackIdentityRepository identityRepository;

    public JpaCanonicalTrackIdentityStore(
        CanonicalTrackRepository trackRepository,
        CanonicalTrackIdentityRepository identityRepository
    ) {
        this.trackRepository = trackRepository;
        this.identityRepository = identityRepository;
    }

    @Override
    @Transactional
    public UpsertResult upsertIdentity(Draft draft) {
        String source = CanonicalTrackIdentityStore.normalizeSource(draft.source());
        String kind = CanonicalTrackIdentityStore.normalizeIdentityKind(draft.identityKind());
        String value = CanonicalTrackIdentityStore.normalizeIdentityValue(kind, draft.identityValue());
        Optional<CanonicalTrackIdentityEntity> existing = identityRepository
            .findFirstBySourceAndIdentityKindAndIdentityValueAndStatus(source, kind, value, STATUS_ACTIVE);
        if (existing.isPresent()) {
            CanonicalTrackEntity track = trackRepository.findById(existing.get().getCanonicalTrackId())
                .orElseThrow(() -> new IllegalStateException("canonical track was not found: " + existing.get().getCanonicalTrackId()));
            return new UpsertResult(track.toEntry(), existing.get().toEntry(), false, false);
        }

        Instant now = draft.now() == null ? Instant.now() : draft.now();
        CanonicalTrackEntity track = trackRepository.save(new CanonicalTrackEntity(
            normalizeDisplayTitle(draft.displayTitle()),
            normalizeOptional(draft.displayArtistName()),
            normalizeOptional(draft.releaseYear()),
            normalizeOptional(draft.releaseCountry()),
            now
        ));
        CanonicalTrackIdentityEntity identity = identityRepository.save(new CanonicalTrackIdentityEntity(
            track.getId(),
            kind,
            value,
            source,
            draft.confidenceScore(),
            draft.createdFromCandidateId(),
            now
        ));
        return new UpsertResult(track.toEntry(), identity.toEntry(), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityEntry> findActiveIdentity(String source, String identityKind, String identityValue) {
        String normalizedSource = CanonicalTrackIdentityStore.normalizeSource(source);
        String normalizedKind = CanonicalTrackIdentityStore.normalizeIdentityKind(identityKind);
        String normalizedValue = CanonicalTrackIdentityStore.normalizeIdentityValue(normalizedKind, identityValue);
        return identityRepository
            .findFirstBySourceAndIdentityKindAndIdentityValueAndStatus(
                normalizedSource,
                normalizedKind,
                normalizedValue,
                STATUS_ACTIVE
            )
            .map(CanonicalTrackIdentityEntity::toEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityEntry> findIdentitiesByCanonicalTrackId(Long canonicalTrackId) {
        return identityRepository.findByCanonicalTrackIdOrderByCreatedAtAscIdAsc(canonicalTrackId).stream()
            .map(CanonicalTrackIdentityEntity::toEntry)
            .toList();
    }

    @Override
    @Transactional
    public CanonicalTrackEntry fillReleaseContextIfMissing(
        Long canonicalTrackId,
        String releaseYear,
        String releaseCountry,
        Instant now
    ) {
        CanonicalTrackEntity track = trackRepository.findById(canonicalTrackId)
            .orElseThrow(() -> new IllegalArgumentException("canonical track was not found: " + canonicalTrackId));
        Instant resolvedNow = now == null ? Instant.now() : now;
        track.fillReleaseContextIfMissing(
            normalizeOptional(releaseYear),
            normalizeOptional(releaseCountry),
            resolvedNow
        );
        return trackRepository.save(track).toEntry();
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
