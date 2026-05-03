package io.myforevermusic.api.modules.platform.application;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.presentation.LastFmScrobbleBootstrapResponse;
import io.myforevermusic.api.modules.platform.presentation.LastFmScrobbleSyncRequest;
import io.myforevermusic.api.modules.platform.presentation.LastFmScrobbleSyncResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LastFmScrobbleSyncService {

    private final AuthAccountStore authAccountStore;
    private final LastFmScrobbleStore lastFmScrobbleStore;
    private final LastFmWebApiClient lastFmWebApiClient;

    public LastFmScrobbleSyncService(
        AuthAccountStore authAccountStore,
        LastFmScrobbleStore lastFmScrobbleStore,
        LastFmWebApiClient lastFmWebApiClient
    ) {
        this.authAccountStore = authAccountStore;
        this.lastFmScrobbleStore = lastFmScrobbleStore;
        this.lastFmWebApiClient = lastFmWebApiClient;
    }

    public LastFmScrobbleBootstrapResponse getBootstrap(String userId) {
        AuthRegisteredAccount account = findAccount(userId);
        LastFmScrobbleStore.ScrobbleSnapshot snapshot = lastFmScrobbleStore.getSnapshot(userId, 10);

        return new LastFmScrobbleBootstrapResponse(
            "api",
            "ok",
            Instant.now(),
            new LastFmScrobbleBootstrapResponse.BootstrapUser(
                account.userId(),
                account.lastFmUsername(),
                account.lastFmConnectedAt()
            ),
            new LastFmScrobbleBootstrapResponse.BootstrapSummary(
                snapshot.storedCount(),
                snapshot.lastSyncedAt(),
                snapshot.recentScrobbles().size(),
                account.lastFmUsername() == null || account.lastFmUsername().isBlank()
                    ? "Save a Last.fm profile first, then sync recent scrobbles."
                    : snapshot.storedCount() == 0
                        ? "Last.fm profile is saved. Run the first scrobble sync to build a listening history snapshot."
                        : "Recent Last.fm scrobbles are stored and ready for future EMS/GMS modeling."
            ),
            snapshot.recentScrobbles().stream()
                .map(this::toScrobbleItem)
                .toList()
        );
    }

    public LastFmScrobbleSyncResponse syncScrobbles(LastFmScrobbleSyncRequest request) {
        AuthRegisteredAccount account = findAccount(request.userId());
        String username = requireLastFmUsername(account);
        Instant syncedAt = Instant.now();

        List<LastFmWebApiClient.LastFmRecentTrack> recentTracks = lastFmWebApiClient.getRecentTracks(username, request.limit());
        int skippedNowPlayingCount = (int) recentTracks.stream()
            .filter(LastFmWebApiClient.LastFmRecentTrack::nowPlaying)
            .count();

        List<LastFmScrobbleStore.StoredScrobble> scrobbles = recentTracks.stream()
            .filter(track -> track.playedAt() != null)
            .map(track -> new LastFmScrobbleStore.StoredScrobble(
                account.userId(),
                username,
                track.trackName(),
                track.artistName(),
                track.albumName(),
                track.trackUrl(),
                track.imageUrl(),
                track.playedAt(),
                track.loved(),
                syncedAt
            ))
            .toList();

        LastFmScrobbleStore.ScrobbleSaveResult saveResult = lastFmScrobbleStore.saveScrobbles(
            account.userId(),
            username,
            syncedAt,
            scrobbles
        );

        List<String> notes = new java.util.ArrayList<>();
        notes.add("Stored scrobbles are deduplicated by username, played_at, artist, and track name.");
        if (skippedNowPlayingCount > 0) {
            notes.add("Now playing rows were skipped because they do not provide a stable scrobble timestamp.");
        }

        return new LastFmScrobbleSyncResponse(
            "api",
            "synced",
            syncedAt,
            new LastFmScrobbleSyncResponse.SyncResult(
                account.userId(),
                username,
                recentTracks.size(),
                saveResult.insertedCount(),
                saveResult.duplicateCount(),
                skippedNowPlayingCount,
                saveResult.snapshot().storedCount(),
                saveResult.snapshot().lastSyncedAt()
            ),
            saveResult.snapshot().recentScrobbles().stream()
                .map(this::toScrobbleItem)
                .toList(),
            List.copyOf(notes)
        );
    }

    private AuthRegisteredAccount findAccount(String userId) {
        return authAccountStore.findByUserId(userId)
            .orElseThrow(() -> new ApiResourceNotFoundException("No registered account found for user: %s".formatted(userId)));
    }

    private String requireLastFmUsername(AuthRegisteredAccount account) {
        if (account.lastFmUsername() == null || account.lastFmUsername().isBlank()) {
            throw new IllegalArgumentException("No Last.fm profile is saved for this account yet.");
        }
        return account.lastFmUsername();
    }

    private LastFmScrobbleBootstrapResponse.ScrobbleItem toScrobbleItem(LastFmScrobbleStore.StoredScrobble scrobble) {
        return new LastFmScrobbleBootstrapResponse.ScrobbleItem(
            scrobble.trackName(),
            scrobble.artistName(),
            scrobble.albumName(),
            scrobble.trackUrl(),
            scrobble.imageUrl(),
            scrobble.playedAt(),
            scrobble.loved(),
            scrobble.syncedAt()
        );
    }
}
