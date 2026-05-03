package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsCatalogPlaylistRepository extends JpaRepository<PmsCatalogPlaylistEntity, String> {

    List<PmsCatalogPlaylistEntity> findAllByOrderByDisplayOrderAscIdAsc();

    List<PmsCatalogPlaylistEntity> findAllByOwnerUserIdOrderByDisplayOrderAscIdAsc(String ownerUserId);
}
