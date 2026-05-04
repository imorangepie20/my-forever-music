package io.myforevermusic.api.modules.pms.infrastructure.local;

import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryPmsUserLibraryStore implements PmsUserLibraryStore {

    private final Map<String, Map<String, LibraryPlaylistState>> libraryByUserId = new ConcurrentHashMap<>();

    @Override
    public List<LibraryPlaylistState> findPlaylists(String userId) {
        Map<String, LibraryPlaylistState> playlists = libraryByUserId.get(userId);
        if (playlists == null || playlists.isEmpty()) {
            return List.of();
        }

        return playlists.values().stream()
            .sorted(Comparator.comparing(LibraryPlaylistState::lastSyncedAt).reversed()
                .thenComparing(LibraryPlaylistState::playlistId))
            .toList();
    }

    @Override
    public List<LibraryPlaylistState> savePlaylists(String userId, List<LibraryPlaylistState> playlists) {
        Map<String, LibraryPlaylistState> bucket = libraryByUserId.computeIfAbsent(
            userId,
            ignored -> new ConcurrentHashMap<>()
        );

        for (LibraryPlaylistState playlist : playlists) {
            bucket.put(playlist.playlistId(), playlist);
        }

        return new LinkedHashMap<>(bucket).values().stream()
            .sorted(Comparator.comparing(LibraryPlaylistState::lastSyncedAt).reversed()
                .thenComparing(LibraryPlaylistState::playlistId))
            .toList();
    }
}
