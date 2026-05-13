package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CanonicalTrackRepository extends JpaRepository<CanonicalTrackEntity, Long> {
}
