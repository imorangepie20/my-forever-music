package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMusicEventRepository extends JpaRepository<UserMusicEventEntity, Long> {

    List<UserMusicEventEntity> findByUserIdOrderByOccurredAtDescEventIdDesc(String userId);
}
