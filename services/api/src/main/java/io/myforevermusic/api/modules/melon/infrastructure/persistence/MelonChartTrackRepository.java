package io.myforevermusic.api.modules.melon.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MelonChartTrackRepository extends JpaRepository<MelonChartTrackEntity, Long> {

    List<MelonChartTrackEntity> findAllByOrderByRankAsc(Pageable pageable);

    List<MelonChartTrackEntity> findAllByOrderByRankAsc();
}
