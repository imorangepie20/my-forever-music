package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsCatalogTrackRepository extends JpaRepository<PmsCatalogTrackEntity, String> {
}
