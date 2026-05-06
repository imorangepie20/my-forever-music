package io.myforevermusic.api.modules.pms.infrastructure.local;

import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryPmsPersonalPlaylistStore implements PmsPersonalPlaylistStore {

    private final Map<String, Map<String, PersonalPlaylistState>> playlistsByUserId = new ConcurrentHashMap<>();

    @Override
    public List<PersonalPlaylistState> findPlaylists(String userId) {
        Map<String, PersonalPlaylistState> playlists = playlistsByUserId.get(userId);
        if (playlists == null || playlists.isEmpty()) {
            return List.of();
        }

        return playlists.values().stream()
            .sorted(Comparator.comparing(PersonalPlaylistState::updatedAt).reversed()
                .thenComparing(PersonalPlaylistState::playlistId))
            .toList();
    }

    @Override
    public Optional<PersonalPlaylistState> findPlaylist(String userId, String playlistId) {
        Map<String, PersonalPlaylistState> playlists = playlistsByUserId.get(userId);
        if (playlists == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playlists.get(playlistId));
    }

    @Override
    public PersonalPlaylistState createPlaylist(CreatePlaylistDraft draft) {
        Map<String, PersonalPlaylistState> playlists = playlistsByUserId.computeIfAbsent(
            draft.userId(),
            ignored -> new ConcurrentHashMap<>()
        );
        Instant now = Instant.now();
        return playlists.computeIfAbsent(
            draft.playlistId(),
            ignored -> new PersonalPlaylistState(
                draft.userId(),
                draft.playlistId(),
                draft.title(),
                draft.description(),
                now,
                now,
                List.of()
            )
        );
    }

    @Override
    public PersonalPlaylistState addTrack(AddTrackDraft draft) {
        PersonalPlaylistState playlist = findPlaylist(draft.userId(), draft.playlistId())
            .orElseThrow(() -> new IllegalArgumentException("Target personal playlist was not found."));
        boolean alreadySaved = playlist.tracks().stream()
            .anyMatch(track -> track.trackId().equals(draft.track().trackId()));
        if (alreadySaved) {
            return playlist;
        }

        List<PersonalTrackState> tracks = new ArrayList<>(playlist.tracks());
        PersonalTrackState track = new PersonalTrackState(
            draft.track().trackId(),
            draft.track().title(),
            draft.track().artistName(),
            draft.track().sourcePlatform(),
            draft.track().albumTitle(),
            draft.track().albumImageUrl(),
            draft.track().platformExternalUrl(),
            draft.track().platformUri(),
            draft.track().previewUrl(),
            draft.track().spotifyTrackId(),
            draft.track().durationMs(),
            tracks.size() + 1,
            draft.sourceContext(),
            Instant.now()
        );
        tracks.add(track);

        PersonalPlaylistState updated = new PersonalPlaylistState(
            playlist.userId(),
            playlist.playlistId(),
            playlist.title(),
            playlist.description(),
            playlist.createdAt(),
            Instant.now(),
            List.copyOf(tracks)
        );
        playlistsByUserId.computeIfAbsent(draft.userId(), ignored -> new LinkedHashMap<>())
            .put(draft.playlistId(), updated);
        return updated;
    }
}
