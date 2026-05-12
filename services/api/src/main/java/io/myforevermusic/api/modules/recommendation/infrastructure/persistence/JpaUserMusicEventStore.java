package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaUserMusicEventStore implements UserMusicEventStore {

    private final UserMusicEventRepository repository;

    public JpaUserMusicEventStore(UserMusicEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public StoredEvent save(EventDraft draft) {
        return repository.save(new UserMusicEventEntity(draft)).toState();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> findRecentByUserId(String userId, int limit) {
        return repository.findByUserIdOrderByOccurredAtDescEventIdDesc(userId).stream()
            .limit(Math.max(0, limit))
            .map(UserMusicEventEntity::toState)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findActiveUserIds(Instant since, int limit) {
        return repository.findActiveUserIds(since, PageRequest.of(0, Math.max(1, limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public long countEventsByUserIdAfter(String userId, Instant since) {
        return repository.countByUserIdAndOccurredAtGreaterThanEqual(userId, since);
    }
}
