package io.myforevermusic.api.modules.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTrackLikeRepository extends JpaRepository<UserTrackLikeEntity, Long> {

    Optional<UserTrackLikeEntity> findByUserIdAndSourcePlatformAndExternalTrackId(
        String userId,
        String sourcePlatform,
        String externalTrackId
    );

    void deleteByUserIdAndSourcePlatformAndExternalTrackId(
        String userId,
        String sourcePlatform,
        String externalTrackId
    );

    List<UserTrackLikeEntity> findByUserIdOrderByLikedAtDesc(String userId, Pageable pageable);

    long countByUserId(String userId);
}
