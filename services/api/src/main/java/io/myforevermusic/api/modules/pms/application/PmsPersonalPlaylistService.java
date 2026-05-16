package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.presentation.PmsPersonalPlaylistBootstrapResponse;
import io.myforevermusic.api.modules.pms.presentation.PmsPersonalPlaylistCreateRequest;
import io.myforevermusic.api.modules.pms.presentation.PmsPersonalPlaylistCommandResponse;
import io.myforevermusic.api.modules.pms.presentation.PmsPersonalPlaylistTrackSaveRequest;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventService;
import io.myforevermusic.api.modules.recommendation.presentation.UserMusicEventRequest;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PmsPersonalPlaylistService {

    private static final String DEFAULT_GMS_PLAYLIST_ID = "personal-saved-gms-recommendations";
    private static final String DEFAULT_GMS_PLAYLIST_TITLE = "Saved GMS Recommendations";

    private final PmsPersonalPlaylistStore personalPlaylistStore;
    private final PmsUserLibraryStore userLibraryStore;
    private final UserMusicEventService userMusicEventService;

    public PmsPersonalPlaylistService(
        PmsPersonalPlaylistStore personalPlaylistStore,
        PmsUserLibraryStore userLibraryStore,
        UserMusicEventService userMusicEventService
    ) {
        this.personalPlaylistStore = personalPlaylistStore;
        this.userLibraryStore = userLibraryStore;
        this.userMusicEventService = userMusicEventService;
    }

    public PmsPersonalPlaylistBootstrapResponse bootstrap(String userId) {
        List<PmsPersonalPlaylistStore.PersonalPlaylistState> playlists = personalPlaylistStore.findPlaylists(userId);
        return PmsPersonalPlaylistBootstrapResponse.from(userId, playlists);
    }

    public PmsPersonalPlaylistCommandResponse createPlaylist(PmsPersonalPlaylistCreateRequest request) {
        String title = normalizeTitle(request.title(), "Untitled PMS Playlist");
        String description = normalizeDescription(request.description());
        String playlistId = buildPlaylistId(title);

        PmsPersonalPlaylistStore.PersonalPlaylistState playlist = personalPlaylistStore.createPlaylist(
            new PmsPersonalPlaylistStore.CreatePlaylistDraft(
                request.userId(),
                playlistId,
                title,
                description
            )
        );

        return PmsPersonalPlaylistCommandResponse.from(
            "created",
            playlist,
            "Personal playlist is ready for saved recommendations and PMS library tracks."
        );
    }

    public PmsPersonalPlaylistCommandResponse saveTrack(PmsPersonalPlaylistTrackSaveRequest request) {
        PmsUserLibraryStore.LibraryTrackState libraryTrack = resolveLibraryTrack(request.userId(), request.trackId());
        PmsPersonalPlaylistStore.PersonalPlaylistState targetPlaylist = resolveTargetPlaylist(request);

        PmsPersonalPlaylistStore.PersonalPlaylistState updatedPlaylist = personalPlaylistStore.addTrack(
            new PmsPersonalPlaylistStore.AddTrackDraft(
                request.userId(),
                targetPlaylist.playlistId(),
                toPersonalTrackState(libraryTrack, request.sourceContext()),
                normalizeSourceContext(request.sourceContext())
            )
        );
        recordAddedToPlaylistEvent(request, targetPlaylist.playlistId(), libraryTrack);

        return PmsPersonalPlaylistCommandResponse.from(
            "saved",
            updatedPlaylist,
            "Track is now saved into a PMS personal playlist."
        );
    }

    private void recordAddedToPlaylistEvent(
        PmsPersonalPlaylistTrackSaveRequest request,
        String targetPlaylistId,
        PmsUserLibraryStore.LibraryTrackState libraryTrack
    ) {
        userMusicEventService.recordEvent(new UserMusicEventRequest(
            request.userId(),
            "added_to_playlist",
            normalizeSourceContext(request.sourceContext()),
            libraryTrack.sourcePlatform(),
            libraryTrack.preferredPlaybackPlatform(),
            libraryTrack.trackId(),
            "track",
            libraryTrack.trackId(),
            targetPlaylistId,
            libraryTrack.externalTrackId(),
            libraryTrack.platformUri(),
            libraryTrack.title(),
            libraryTrack.artistName(),
            libraryTrack.albumTitle(),
            libraryTrack.isrc(),
            trackDurationMs(libraryTrack),
            null,
            null,
            null,
            null,
            Instant.now()
        ));
    }

    private PmsPersonalPlaylistStore.PersonalPlaylistState resolveTargetPlaylist(
        PmsPersonalPlaylistTrackSaveRequest request
    ) {
        if (request.targetPlaylistId() != null && !request.targetPlaylistId().isBlank()) {
            return personalPlaylistStore.findPlaylist(request.userId(), request.targetPlaylistId())
                .orElseThrow(() -> new IllegalArgumentException("Target personal playlist was not found."));
        }

        return personalPlaylistStore.findPlaylist(request.userId(), DEFAULT_GMS_PLAYLIST_ID)
            .orElseGet(() -> personalPlaylistStore.createPlaylist(
                new PmsPersonalPlaylistStore.CreatePlaylistDraft(
                    request.userId(),
                    DEFAULT_GMS_PLAYLIST_ID,
                    normalizeTitle(request.targetPlaylistTitle(), DEFAULT_GMS_PLAYLIST_TITLE),
                    "Tracks saved from GMS recommendation candidates."
                )
            ));
    }

    private PmsUserLibraryStore.LibraryTrackState resolveLibraryTrack(String userId, String trackId) {
        return userLibraryStore.findPlaylists(userId).stream()
            .flatMap(playlist -> playlist.tracks().stream())
            .filter(track -> track.trackId().equals(trackId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Only tracks in the synced PMS user library can be saved into a personal playlist."
            ));
    }

    private PmsPersonalPlaylistStore.PersonalTrackState toPersonalTrackState(
        PmsUserLibraryStore.LibraryTrackState track,
        String sourceContext
    ) {
        Integer durationMs = track.audioFeatures() == null
            ? null
            : track.audioFeatures().getDurationMs();
        String audioFeatureTrackId = track.audioFeatures() == null
            ? null
            : track.audioFeatures().getAudioFeatureTrackId();

        return new PmsPersonalPlaylistStore.PersonalTrackState(
            track.trackId(),
            track.externalTrackId(),
            track.title(),
            track.artistName(),
            track.sourcePlatform(),
            track.albumTitle(),
            track.albumImageUrl(),
            track.platformExternalUrl(),
            track.platformUri(),
            track.previewUrl(),
            track.isrc(),
            audioFeatureTrackId,
            track.spotifyUri(),
            track.tidalTrackId(),
            track.tidalUri(),
            track.preferredPlaybackPlatform(),
            track.playbackTargetStatus(),
            durationMs,
            null,
            normalizeSourceContext(sourceContext),
            Instant.now()
        );
    }

    private Integer trackDurationMs(PmsUserLibraryStore.LibraryTrackState track) {
        return track.audioFeatures() == null
            ? null
            : track.audioFeatures().getDurationMs();
    }

    private String normalizeTitle(String title, String fallback) {
        String normalized = title == null ? "" : title.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalizeDescription(String description) {
        String normalized = description == null ? "" : description.trim();
        return normalized.isBlank() ? "Created inside PMS." : normalized;
    }

    private String normalizeSourceContext(String sourceContext) {
        String normalized = sourceContext == null ? "" : sourceContext.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "pms" : normalized;
    }

    private String buildPlaylistId(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        String slug = normalized.isBlank() ? "playlist" : normalized;
        if (slug.length() > 48) {
            slug = slug.substring(0, 48).replaceAll("-$", "");
        }
        return "personal-%s-%s".formatted(slug, UUID.randomUUID().toString().substring(0, 8));
    }
}
