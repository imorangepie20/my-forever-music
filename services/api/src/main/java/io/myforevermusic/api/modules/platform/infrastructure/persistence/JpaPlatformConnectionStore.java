package io.myforevermusic.api.modules.platform.infrastructure.persistence;

import io.myforevermusic.api.modules.platform.application.PlatformConnectionDraft;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionState;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionStore;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class JpaPlatformConnectionStore implements PlatformConnectionStore {

    private final PlatformAccountConnectionRepository platformAccountConnectionRepository;

    public JpaPlatformConnectionStore(PlatformAccountConnectionRepository platformAccountConnectionRepository) {
        this.platformAccountConnectionRepository = platformAccountConnectionRepository;
    }

    @Override
    public List<PlatformConnectionState> findByUserId(String userId) {
        return platformAccountConnectionRepository.findByUserId(userId)
            .stream()
            .map(PlatformAccountConnectionEntity::toState)
            .toList();
    }

    @Override
    public PlatformConnectionState connect(PlatformConnectionDraft draft) {
        PlatformAccountConnectionEntity entity = platformAccountConnectionRepository
            .findByUserIdAndPlatformId(draft.userId(), draft.platformId())
            .orElseGet(() -> new PlatformAccountConnectionEntity(
                draft.userId(),
                draft.platformId(),
                true,
                "connected",
                draft.connectionMode(),
                draft.externalAccountLabel(),
                draft.scopeSummary(),
                draft.syncReady(),
                draft.connectedAt(),
                draft.updatedAt()
            ));

        entity.markConnected(
            draft.connectionMode(),
            draft.externalAccountLabel(),
            draft.scopeSummary(),
            draft.syncReady(),
            draft.connectedAt(),
            draft.updatedAt()
        );

        return platformAccountConnectionRepository.save(entity).toState();
    }

    @Override
    public PlatformConnectionState disconnect(String userId, String platformId) {
        PlatformAccountConnectionEntity entity = platformAccountConnectionRepository
            .findByUserIdAndPlatformId(userId, platformId)
            .orElseGet(() -> new PlatformAccountConnectionEntity(
                userId,
                platformId,
                false,
                "not_connected",
                "none",
                null,
                null,
                false,
                null,
                Instant.now()
            ));

        entity.markDisconnected(Instant.now());
        return platformAccountConnectionRepository.save(entity).toState();
    }
}
