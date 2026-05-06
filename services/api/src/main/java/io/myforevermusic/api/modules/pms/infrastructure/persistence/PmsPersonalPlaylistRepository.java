package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PmsPersonalPlaylistRepository extends JpaRepository<PmsPersonalPlaylistEntity, Long> {

    List<PmsPersonalPlaylistEntity> findAllByUserIdOrderByUpdatedAtDescPlaylistIdAsc(String userId);

    Optional<PmsPersonalPlaylistEntity> findByUserIdAndPlaylistId(String userId, String playlistId);
}
