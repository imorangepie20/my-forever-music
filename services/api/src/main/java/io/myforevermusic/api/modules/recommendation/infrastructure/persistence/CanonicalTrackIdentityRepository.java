package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanonicalTrackIdentityRepository extends JpaRepository<CanonicalTrackIdentityEntity, Long> {

    Optional<CanonicalTrackIdentityEntity> findFirstBySourceAndIdentityKindAndIdentityValueAndStatus(
        String source,
        String identityKind,
        String identityValue,
        String status
    );

    List<CanonicalTrackIdentityEntity> findByCanonicalTrackIdOrderByCreatedAtAscIdAsc(Long canonicalTrackId);
}
