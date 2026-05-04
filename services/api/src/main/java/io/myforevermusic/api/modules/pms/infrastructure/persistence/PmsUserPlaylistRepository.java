package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsUserPlaylistRepository extends JpaRepository<PmsUserPlaylistEntity, Long> {

    List<PmsUserPlaylistEntity> findAllByUserIdOrderByLastSyncedAtDescPlaylistIdAsc(String userId);

    Optional<PmsUserPlaylistEntity> findByUserIdAndPlaylistId(String userId, String playlistId);
}
