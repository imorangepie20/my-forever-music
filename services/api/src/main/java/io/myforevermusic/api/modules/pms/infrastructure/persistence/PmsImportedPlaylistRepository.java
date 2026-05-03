package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsImportedPlaylistRepository extends JpaRepository<PmsImportedPlaylistEntity, Long> {

    List<PmsImportedPlaylistEntity> findAllByUserIdOrderByImportedAtDescPlaylistIdAsc(String userId);

    Optional<PmsImportedPlaylistEntity> findByUserIdAndPlaylistId(String userId, String playlistId);
}
