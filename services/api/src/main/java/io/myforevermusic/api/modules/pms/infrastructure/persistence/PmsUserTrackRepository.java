package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsUserTrackRepository extends JpaRepository<PmsUserTrackEntity, String> {
}
