package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistTrack;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportStore.ImportedPlaylistState;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportStore.ImportedTrackState;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PmsPlaybackTargetResolverService {

    private static final Logger log = LoggerFactory.getLogger(PmsPlaybackTargetResolverService.class);
    private static final int SEARCH_LIMIT = 5;
    private static final int DURATION_TOLERANCE_MS = 3_000;

    private final PlatformCredentialService platformCredentialService;
    private final SpotifyWebApiClient spotifyWebApiClient;
    private final TidalWebApiClient tidalWebApiClient;

    public PmsPlaybackTargetResolverService(
        PlatformCredentialService platformCredentialService,
        SpotifyWebApiClient spotifyWebApiClient,
        TidalWebApiClient tidalWebApiClient
    ) {
        this.platformCredentialService = platformCredentialService;
        this.spotifyWebApiClient = spotifyWebApiClient;
        this.tidalWebApiClient = tidalWebApiClient;
    }

    public List<ImportedPlaylistState> enrichPlaybackTargets(String userId, List<ImportedPlaylistState> playlists) {
        if (playlists == null || playlists.isEmpty()) {
            return List.of();
        }

        Optional<PlatformAccountCredential> spotifyCredential = platformCredentialService.findUsableCredential(userId, "spotify");
        Optional<PlatformAccountCredential> tidalCredential = platformCredentialService.findUsableCredential(userId, "tidal");

        return playlists.stream()
            .map(playlist -> new ImportedPlaylistState(
                playlist.userId(),
                playlist.playlistId(),
                playlist.externalPlaylistId(),
                playlist.title(),
                playlist.sourcePlatform(),
                playlist.curator(),
                playlist.highlight(),
                playlist.coverImageUrl(),
                playlist.platformExternalUrl(),
                playlist.platformUri(),
                playlist.importedAt(),
                playlist.tracks().stream()
                    .map(track -> enrichTrack(track, spotifyCredential, tidalCredential))
                    .toList()
            ))
            .toList();
    }

    private ImportedTrackState enrichTrack(
        ImportedTrackState track,
        Optional<PlatformAccountCredential> spotifyCredential,
        Optional<PlatformAccountCredential> tidalCredential
    ) {
        ImportedTrackState next = track;
        if ("spotify".equals(track.sourcePlatform()) && !hasText(track.tidalTrackId()) && tidalCredential.isPresent()) {
            next = resolveTidalTarget(track, tidalCredential.get()).orElse(track);
        }
        if ("tidal".equals(next.sourcePlatform()) && !hasText(next.spotifyTrackId()) && spotifyCredential.isPresent()) {
            next = resolveSpotifyTarget(next, spotifyCredential.get()).orElse(next);
        }
        return next;
    }

    private Optional<ImportedTrackState> resolveTidalTarget(ImportedTrackState track, PlatformAccountCredential credential) {
        try {
            return tidalWebApiClient.searchTracks(credential, searchQuery(track), SEARCH_LIMIT).stream()
                .filter(candidate -> isMatch(track, candidate))
                .findFirst()
                .map(candidate -> copyWithTargets(
                    track,
                    track.spotifyTrackId(),
                    track.spotifyUri(),
                    candidate.tidalTrackId(),
                    candidate.tidalUri(),
                    firstNonBlank(track.isrc(), candidate.isrc())
                ));
        } catch (RuntimeException exception) {
            log.warn(
                "TIDAL playback target resolution failed for PMS track {}: {}",
                track.trackId(),
                exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private Optional<ImportedTrackState> resolveSpotifyTarget(ImportedTrackState track, PlatformAccountCredential credential) {
        try {
            return spotifyWebApiClient.searchTracks(credential, searchQuery(track), SEARCH_LIMIT).items().stream()
                .filter(candidate -> isMatch(track, candidate))
                .findFirst()
                .map(candidate -> copyWithTargets(
                    track,
                    candidate.spotifyTrackId(),
                    candidate.spotifyUri(),
                    track.tidalTrackId(),
                    track.tidalUri(),
                    firstNonBlank(track.isrc(), candidate.isrc())
                ));
        } catch (RuntimeException exception) {
            log.warn(
                "Spotify playback target resolution failed for PMS track {}: {}",
                track.trackId(),
                exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private boolean isMatch(ImportedTrackState source, TidalPlaylistTrack candidate) {
        if (sameIsrc(source.isrc(), candidate.isrc())) {
            return true;
        }
        return sameTitle(source.title(), candidate.title())
            && sameArtist(source.artistName(), candidate.artistName())
            && closeDuration(durationMs(source), candidate.durationMs() > 0 ? candidate.durationMs() : null);
    }

    private boolean isMatch(ImportedTrackState source, SpotifyPlaylistTrack candidate) {
        if (sameIsrc(source.isrc(), candidate.isrc())) {
            return true;
        }
        return sameTitle(source.title(), candidate.title())
            && sameArtist(source.artistName(), candidate.artistName())
            && closeDuration(durationMs(source), candidate.durationMs());
    }

    private ImportedTrackState copyWithTargets(
        ImportedTrackState track,
        String spotifyTrackId,
        String spotifyUri,
        String tidalTrackId,
        String tidalUri,
        String isrc
    ) {
        return new ImportedTrackState(
            track.trackId(),
            track.externalTrackId(),
            track.title(),
            track.artistName(),
            track.sourcePlatform(),
            track.primaryGenre(),
            track.albumTitle(),
            track.albumImageUrl(),
            track.platformExternalUrl(),
            track.platformUri(),
            track.previewUrl(),
            isrc,
            spotifyTrackId,
            spotifyUri,
            tidalTrackId,
            tidalUri,
            track.preferredPlaybackPlatform(),
            track.playbackTargetStatus(),
            track.sortOrder(),
            track.seed(),
            track.audioFeatures()
        );
    }

    private String searchQuery(ImportedTrackState track) {
        return "%s %s".formatted(track.title(), track.artistName()).trim();
    }

    private Integer durationMs(ImportedTrackState track) {
        return track.audioFeatures() == null ? null : track.audioFeatures().getDurationMs();
    }

    private boolean sameIsrc(String left, String right) {
        return hasText(left) && hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean sameTitle(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private boolean sameArtist(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft.equals(normalizedRight)
            || normalizedLeft.contains(normalizedRight)
            || normalizedRight.contains(normalizedLeft);
    }

    private boolean closeDuration(Integer left, Integer right) {
        if (left == null || right == null) {
            return false;
        }
        return Math.abs(left - right) <= DURATION_TOLERANCE_MS;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
            .replaceAll("[^a-z0-9가-힣]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : (hasText(second) ? second : null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
