package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import io.myforevermusic.api.modules.recommendation.application.SasrecAutoTrainLogStore;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaSasrecAutoTrainLogStore implements SasrecAutoTrainLogStore {

    private final SasrecAutoTrainLogRepository repository;

    public JpaSasrecAutoTrainLogStore(SasrecAutoTrainLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Entry save(Draft draft) {
        return repository.save(new SasrecAutoTrainLogEntity(draft)).toEntry();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Entry> findLatestByUserId(String userId) {
        return repository.findFirstByUserIdOrderByTrainedAtDesc(userId).map(SasrecAutoTrainLogEntity::toEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entry> findRecentByUserId(String userId, int limit) {
        return repository.findRecentByUserId(userId, PageRequest.of(0, Math.max(1, limit))).stream()
            .map(SasrecAutoTrainLogEntity::toEntry)
            .toList();
    }
}
