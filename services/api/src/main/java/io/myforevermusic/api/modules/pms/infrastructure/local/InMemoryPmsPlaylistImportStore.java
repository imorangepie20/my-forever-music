package io.myforevermusic.api.modules.pms.infrastructure.local;

import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportStore;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryPmsPlaylistImportStore implements PmsPlaylistImportStore {

    private final Map<String, Map<String, ImportedPlaylistState>> importsByUserId = new ConcurrentHashMap<>();

    @Override
    public List<ImportedPlaylistState> findImportedPlaylists(String userId) {
        Map<String, ImportedPlaylistState> playlists = importsByUserId.get(userId);
        if (playlists == null || playlists.isEmpty()) {
            return List.of();
        }

        return playlists.values().stream()
            .sorted(Comparator.comparing(ImportedPlaylistState::importedAt).reversed()
                .thenComparing(ImportedPlaylistState::playlistId))
            .toList();
    }

    @Override
    public List<ImportedPlaylistState> saveImportedPlaylists(String userId, List<ImportedPlaylistState> playlists) {
        Map<String, ImportedPlaylistState> bucket = importsByUserId.computeIfAbsent(
            userId,
            ignored -> new ConcurrentHashMap<>()
        );

        for (ImportedPlaylistState playlist : playlists) {
            bucket.put(playlist.playlistId(), playlist);
        }

        return new LinkedHashMap<>(bucket).values().stream()
            .sorted(Comparator.comparing(ImportedPlaylistState::importedAt).reversed()
                .thenComparing(ImportedPlaylistState::playlistId))
            .toList();
    }
}
