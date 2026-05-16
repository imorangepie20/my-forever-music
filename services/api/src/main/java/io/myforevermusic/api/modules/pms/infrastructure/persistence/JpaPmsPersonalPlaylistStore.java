package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistStore;
import io.myforevermusic.api.modules.pms.application.PmsUserLibraryStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaPmsPersonalPlaylistStore implements PmsPersonalPlaylistStore {

    private final PmsPersonalPlaylistRepository playlistRepository;
    private final PmsPersonalPlaylistTrackRepository playlistTrackRepository;
    private final PmsUserTrackRepository userTrackRepository;

    public JpaPmsPersonalPlaylistStore(
        PmsPersonalPlaylistRepository playlistRepository,
        PmsPersonalPlaylistTrackRepository playlistTrackRepository,
        PmsUserTrackRepository userTrackRepository
    ) {
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.userTrackRepository = userTrackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalPlaylistState> findPlaylists(String userId) {
        return playlistRepository.findAllByUserIdOrderByUpdatedAtDescPlaylistIdAsc(userId).stream()
            .map(this::toState)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonalPlaylistState> findPlaylist(String userId, String playlistId) {
        return playlistRepository.findByUserIdAndPlaylistId(userId, playlistId)
            .map(this::toState);
    }

    @Override
    @Transactional
    public PersonalPlaylistState createPlaylist(CreatePlaylistDraft draft) {
        PmsPersonalPlaylistEntity playlist = playlistRepository
            .findByUserIdAndPlaylistId(draft.userId(), draft.playlistId())
            .orElseGet(() -> playlistRepository.save(new PmsPersonalPlaylistEntity(draft)));
        return toState(playlist);
    }

    @Override
    @Transactional
    public PersonalPlaylistState addTrack(AddTrackDraft draft) {
        PmsPersonalPlaylistEntity playlist = playlistRepository
            .findByUserIdAndPlaylistId(draft.userId(), draft.playlistId())
            .orElseThrow(() -> new IllegalArgumentException("Target personal playlist was not found."));
        PmsUserTrackEntity track = userTrackRepository.findById(draft.track().trackId())
            .orElseGet(() -> userTrackRepository.save(new PmsUserTrackEntity(toLibraryTrackState(draft.track()))));

        Optional<PmsPersonalPlaylistTrackEntity> existingLink = playlistTrackRepository
            .findByPlaylist_PersonalPlaylistIdAndTrack_TrackId(playlist.getPersonalPlaylistId(), track.getTrackId());
        if (existingLink.isEmpty()) {
            int sortOrder = (int) playlistTrackRepository.countByPlaylist_PersonalPlaylistId(
                playlist.getPersonalPlaylistId()
            ) + 1;
            playlistTrackRepository.save(new PmsPersonalPlaylistTrackEntity(
                null,
                playlist,
                track,
                sortOrder,
                draft.sourceContext(),
                Instant.now()
            ));
            playlist.markUpdated(Instant.now());
            playlistRepository.save(playlist);
        }

        return toState(playlist);
    }

    private PersonalPlaylistState toState(PmsPersonalPlaylistEntity playlist) {
        List<PersonalTrackState> tracks = playlistTrackRepository
            .findByPlaylist_PersonalPlaylistIdOrderBySortOrderAscPersonalPlaylistTrackIdAsc(
                playlist.getPersonalPlaylistId()
            ).stream()
            .map(this::toTrackState)
            .toList();

        return new PersonalPlaylistState(
            playlist.getUserId(),
            playlist.getPlaylistId(),
            playlist.getTitle(),
            playlist.getDescription(),
            playlist.getCreatedAt(),
            playlist.getUpdatedAt(),
            tracks
        );
    }

    private PersonalTrackState toTrackState(PmsPersonalPlaylistTrackEntity link) {
        PmsUserTrackEntity track = link.getTrack();
        PmsTrackAudioFeatures features = track.getAudioFeatures();

        return new PersonalTrackState(
            track.getTrackId(),
            track.getExternalTrackId(),
            track.getTitle(),
            track.getArtistName(),
            track.getSourcePlatform(),
            track.getAlbumTitle(),
            track.getAlbumImageUrl(),
            track.getPlatformExternalUrl(),
            track.getPlatformUri(),
            track.getPreviewUrl(),
            track.getIsrc(),
            features == null ? null : features.getAudioFeatureTrackId(),
            track.getSpotifyUri(),
            track.getTidalTrackId(),
            track.getTidalUri(),
            track.getPreferredPlaybackPlatform(),
            track.getPlaybackTargetStatus(),
            features == null ? null : features.getDurationMs(),
            link.getSortOrder(),
            link.getSourceContext(),
            link.getAddedAt()
        );
    }

    private PmsUserLibraryStore.LibraryTrackState toLibraryTrackState(PersonalTrackState track) {
        return new PmsUserLibraryStore.LibraryTrackState(
            track.trackId(),
            firstNonBlank(track.externalTrackId(), track.spotifyTrackId(), track.tidalTrackId(), track.trackId()),
            track.title(),
            track.artistName(),
            track.sourcePlatform(),
            null,
            track.albumTitle(),
            track.albumImageUrl(),
            track.platformExternalUrl(),
            track.platformUri(),
            track.previewUrl(),
            track.isrc(),
            track.spotifyTrackId(),
            track.spotifyUri(),
            track.tidalTrackId(),
            track.tidalUri(),
            track.preferredPlaybackPlatform(),
            track.playbackTargetStatus(),
            track.sortOrder() == null ? 0 : track.sortOrder(),
            false,
            new PmsTrackAudioFeatures(
                track.spotifyTrackId(),
                "pms-personal-playlist",
                false,
                null,
                null,
                track.spotifyUri(),
                "audio_features",
                track.durationMs(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
