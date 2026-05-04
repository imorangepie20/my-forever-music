package io.myforevermusic.api.modules.platform.infrastructure.local;

import io.myforevermusic.api.modules.platform.application.PlatformConnectionDraft;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionState;
import io.myforevermusic.api.modules.platform.application.PlatformConnectionStore;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryPlatformConnectionStore implements PlatformConnectionStore {

    private final ConcurrentMap<String, StoredConnection> connections = new ConcurrentHashMap<>();

    @Override
    public List<PlatformConnectionState> findByUserId(String userId) {
        return connections.values()
            .stream()
            .filter(connection -> connection.userId().equals(userId))
            .map(StoredConnection::toState)
            .toList();
    }

    @Override
    public PlatformConnectionState connect(PlatformConnectionDraft draft) {
        StoredConnection connection = new StoredConnection(
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
        );

        connections.put(key(draft.userId(), draft.platformId()), connection);
        return connection.toState();
    }

    @Override
    public PlatformConnectionState disconnect(String userId, String platformId) {
        String key = key(userId, platformId);
        StoredConnection current = connections.get(key);
        Instant now = Instant.now();

        StoredConnection disconnected = current == null
            ? new StoredConnection(userId, platformId, false, "not_connected", "none", null, null, false, null, now)
            : current.withDisconnected(now);

        connections.put(key, disconnected);
        return disconnected.toState();
    }

    private String key(String userId, String platformId) {
        return userId + "::" + platformId;
    }

    private record StoredConnection(
        String userId,
        String platformId,
        boolean connected,
        String connectionStatus,
        String connectionMode,
        String externalAccountLabel,
        String scopeSummary,
        boolean syncReady,
        Instant connectedAt,
        Instant updatedAt
    ) {

        private PlatformConnectionState toState() {
            return new PlatformConnectionState(
                userId,
                platformId,
                connected,
                connectionStatus,
                connectionMode,
                externalAccountLabel,
                scopeSummary,
                syncReady,
                connectedAt,
                updatedAt
            );
        }

        private StoredConnection withDisconnected(Instant updatedAt) {
            return new StoredConnection(
                userId,
                platformId,
                false,
                "not_connected",
                connectionMode,
                externalAccountLabel,
                scopeSummary,
                false,
                connectedAt,
                updatedAt
            );
        }
    }
}
