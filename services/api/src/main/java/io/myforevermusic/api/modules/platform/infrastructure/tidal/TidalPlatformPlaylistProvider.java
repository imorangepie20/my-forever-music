package io.myforevermusic.api.modules.platform.infrastructure.tidal;

import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProvider;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsAudioFeaturesSnapshot;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsTrackLookupRequest;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistSummary;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalUserProfile;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidatePlaylist;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidateTrack;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * TIDAL PMS Playlist Provider
 *
 * <p>Implements PMS playlist import from TIDAL using the TIDAL Open API v2.
 *
 * <p>Key characteristics:
 * <ul>
 *   <li>Uses OAuth 2.1 with PKCE for authentication</li>
 *   <li>Fetches user playlists via /userCollectionPlaylists</li>
 *   <li>Fetches playlist items via /playlists/{id}?include=items,items.artists,items.albums</li>
 *   <li>Resolves audio features through ReccoBeats ISRC matching</li>
 * </ul>
 */
@Component
public class TidalPlatformPlaylistProvider implements PlatformPlaylistProvider {

    private static final Logger log = LoggerFactory.getLogger(TidalPlatformPlaylistProvider.class);

    private final TidalWebApiClient tidalWebApiClient;
    private final ReccoBeatsAudioFeaturesClient reccoBeatsAudioFeaturesClient;

    public TidalPlatformPlaylistProvider(
        TidalWebApiClient tidalWebApiClient,
        ReccoBeatsAudioFeaturesClient reccoBeatsAudioFeaturesClient
    ) {
        this.tidalWebApiClient = tidalWebApiClient;
        this.reccoBeatsAudioFeaturesClient = reccoBeatsAudioFeaturesClient;
    }

    @Override
    public boolean supports(String platformId, PlatformAccountCredential credential) {
        return "tidal".equals(platformId)
            && credential != null
            && credential.authorizationMode() != null
            && credential.authorizationMode().startsWith("tidal")
            && credential.accessToken() != null
            && !credential.accessToken().isBlank();
    }

    @Override
    public List<ImportCandidatePlaylist> listImportablePlaylists(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential
    ) {
        try {
            TidalUserProfile profile = tidalWebApiClient.getCurrentUserProfile(credential);
            return tidalWebApiClient.getUserPlaylists(credential).stream()
                .filter(playlist -> isOwnedByUser(profile, playlist))
                .map(this::toImportCandidatePlaylist)
                .toList();
        } catch (Exception exception) {
            log.warn("TIDAL playlist listing failed: {}", exception.getMessage());
            return List.of();
        }
    }

    @Override
    public List<ImportCandidatePlaylist> loadPlaylistsForImport(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential,
        List<String> externalPlaylistIds
    ) {
        try {
            return externalPlaylistIds.stream()
                .map(playlistId -> loadSinglePlaylist(credential, playlistId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        } catch (Exception exception) {
            log.warn("TIDAL playlist loading failed: {}", exception.getMessage());
            return List.of();
        }
    }

    private Optional<ImportCandidatePlaylist> loadSinglePlaylist(
        PlatformAccountCredential credential,
        String playlistId
    ) {
        try {
            TidalPlaylistSummary summary = tidalWebApiClient.getUserPlaylists(credential).stream()
                .filter(p -> p.playlistId().equals(playlistId))
                .findFirst()
                .orElse(null);

            if (summary == null) {
                log.warn("TIDAL playlist not found: {}", playlistId);
                return Optional.empty();
            }

            List<TidalPlaylistTrack> tracks = tidalWebApiClient.getPlaylistTracks(credential, playlistId);
            Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByTrackId = resolveAudioFeatures(tracks);
            Instant resolvedAt = Instant.now();
            List<ImportCandidateTrack> importTracks = IntStream.range(0, tracks.size())
                .mapToObj(index -> toImportCandidateTrack(
                    tracks.get(index),
                    index < 2,
                    audioFeaturesByTrackId.get(tracks.get(index).tidalTrackId()),
                    resolvedAt
                ))
                .toList();

            return Optional.of(new ImportCandidatePlaylist(
                summary.playlistId(),
                summary.name(),
                "tidal",
                "TIDAL User",
                normalizeDescription(summary.description()),
                summary.coverImageUrl(),
                summary.externalUrl(),
                "tidal:playlist:%s".formatted(summary.uuid()),
                importTracks.size(),
                importTracks
            ));
        } catch (Exception exception) {
            log.warn("Failed to load TIDAL playlist {}: {}", playlistId, exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean isOwnedByUser(TidalUserProfile profile, TidalPlaylistSummary playlist) {
        // TIDAL doesn't explicitly provide owner ID in the playlist summary
        // For now, we assume all fetched playlists are accessible
        return true;
    }

    private ImportCandidatePlaylist toImportCandidatePlaylist(TidalPlaylistSummary playlist) {
        return new ImportCandidatePlaylist(
            playlist.playlistId(),
            playlist.name(),
            "tidal",
            "TIDAL User",
            normalizeDescription(playlist.description()),
            playlist.coverImageUrl(),
            playlist.externalUrl(),
            "tidal:playlist:%s".formatted(playlist.uuid()),
            playlist.trackCount(),
            List.of() // Empty tracks for summary view
        );
    }

    private ImportCandidateTrack toImportCandidateTrack(
        TidalPlaylistTrack track,
        boolean seed,
        ReccoBeatsAudioFeaturesSnapshot audioFeaturesSnapshot,
        Instant resolvedAt
    ) {
        return new ImportCandidateTrack(
            track.tidalTrackId(),
            track.title(),
            track.artistName(),
            null, // genre - will be inferred later
            track.albumTitle(),
            track.albumImageUrl(),
            track.externalUrl(),
            track.tidalUri(),
            track.previewUrl(),
            seed,
            resolveTrackAudioFeatures(track, audioFeaturesSnapshot, resolvedAt)
        );
    }

    private Map<String, ReccoBeatsAudioFeaturesSnapshot> resolveAudioFeatures(List<TidalPlaylistTrack> tracks) {
        List<ReccoBeatsTrackLookupRequest> lookupRequests = tracks.stream()
            .filter(track -> track != null && hasText(track.tidalTrackId()) && hasText(track.isrc()))
            .map(track -> new ReccoBeatsTrackLookupRequest(
                track.tidalTrackId(),
                track.title(),
                track.artistName(),
                track.durationMs() > 0 ? track.durationMs() : null,
                track.isrc()
            ))
            .toList();

        if (lookupRequests.isEmpty()) {
            return Map.of();
        }

        try {
            return reccoBeatsAudioFeaturesClient.getAudioFeaturesForExternalTracksByIsrc(lookupRequests);
        } catch (RuntimeException exception) {
            log.warn(
                "ReccoBeats ISRC audio-features lookup for TIDAL failed: {}. Proceeding with placeholders.",
                exception.getMessage()
            );
            return Map.of();
        }
    }

    private PmsTrackAudioFeatures resolveTrackAudioFeatures(
        TidalPlaylistTrack track,
        ReccoBeatsAudioFeaturesSnapshot snapshot,
        Instant resolvedAt
    ) {
        Integer durationMs = track.durationMs() > 0 ? track.durationMs() : null;
        if (snapshot != null) {
            return new PmsTrackAudioFeatures(
                snapshot.spotifyTrackId(),
                "reccobeats_isrc_match",
                true,
                null,
                snapshot.spotifyTrackHref(),
                buildSpotifyUri(snapshot.spotifyTrackId()),
                "audio_features",
                durationMs,
                snapshot.musicalKey(),
                snapshot.mode(),
                null,
                snapshot.acousticness(),
                snapshot.danceability(),
                snapshot.energy(),
                snapshot.instrumentalness(),
                snapshot.liveness(),
                snapshot.loudness(),
                snapshot.speechiness(),
                snapshot.tempo(),
                snapshot.valence(),
                snapshot.resolvedAt() == null ? resolvedAt : snapshot.resolvedAt()
            );
        }

        log.info("No ReccoBeats audio features for TIDAL track {} ({}). Storing placeholder.", track.tidalTrackId(), track.title());
        return new PmsTrackAudioFeatures(
            null,
            "unavailable",
            false,
            null,
            null,
            null,
            "audio_features",
            durationMs,
            null, null, null,
            null, null, null, null, null, null, null, null, null,
            resolvedAt
        );
    }

    private String buildSpotifyUri(String spotifyTrackId) {
        return hasText(spotifyTrackId) ? "spotify:track:%s".formatted(spotifyTrackId) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
            ? "Imported from TIDAL."
            : description.trim();
    }
}
