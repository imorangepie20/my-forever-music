package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMusicEventRepository extends JpaRepository<UserMusicEventEntity, Long> {

    List<UserMusicEventEntity> findByUserIdOrderByOccurredAtDescEventIdDesc(String userId);

    @Query("select distinct event.userId from UserMusicEventEntity event "
        + "where event.occurredAt >= :since order by event.userId")
    List<String> findActiveUserIds(@Param("since") Instant since, Pageable pageable);

    long countByUserIdAndOccurredAtGreaterThanEqual(String userId, Instant since);
}
