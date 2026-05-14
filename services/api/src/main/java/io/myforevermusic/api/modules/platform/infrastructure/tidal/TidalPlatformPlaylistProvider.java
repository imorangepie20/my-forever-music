package io.myforevermusic.api.modules.platform.infrastructure.tidal;

import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformProviderOperationException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        TidalUserProfile profile = getCurrentUserProfile(credential);
        return getUserPlaylists(credential, "listing playlists").stream()
            .filter(playlist -> isOwnedByUser(profile, playlist))
            .map(this::toImportCandidatePlaylist)
            .toList();
    }

    @Override
    public List<ImportCandidatePlaylist> loadPlaylistsForImport(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential,
        List<String> externalPlaylistIds
    ) {
        Map<String, TidalPlaylistSummary> playlistsById = getUserPlaylists(credential, "loading playlist summaries")
            .stream()
            .collect(Collectors.toMap(
                TidalPlaylistSummary::playlistId,
                playlist -> playlist,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        return externalPlaylistIds.stream()
            .map(playlistId -> loadSinglePlaylist(credential, playlistId, playlistsById.get(playlistId)))
            .toList();
    }

    private ImportCandidatePlaylist loadSinglePlaylist(
        PlatformAccountCredential credential,
        String playlistId,
        TidalPlaylistSummary summary
    ) {
        if (summary == null) {
            throw new IllegalArgumentException("TIDAL playlist is not available for import: %s".formatted(playlistId));
        }

        List<TidalPlaylistTrack> tracks = getPlaylistTracks(credential, playlistId);
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

        return new ImportCandidatePlaylist(
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
        );
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
            track.isrc(),
            audioFeaturesSnapshot == null ? null : audioFeaturesSnapshot.spotifyTrackId(),
            audioFeaturesSnapshot == null ? null : buildSpotifyUri(audioFeaturesSnapshot.spotifyTrackId()),
            track.tidalTrackId(),
            track.tidalUri(),
            "tidal",
            "native",
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
                "ReccoBeats ISRC audio-features lookup for TIDAL failed: {}. Proceeding with unavailable audio feature snapshots.",
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

        log.info(
            "No ReccoBeats audio features for TIDAL track {} ({}). Storing unavailable audio feature snapshot.",
            track.tidalTrackId(),
            track.title()
        );
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

    private TidalUserProfile getCurrentUserProfile(PlatformAccountCredential credential) {
        try {
            return tidalWebApiClient.getCurrentUserProfile(credential);
        } catch (RuntimeException exception) {
            throw providerOperationFailed("loading user profile", exception);
        }
    }

    private List<TidalPlaylistSummary> getUserPlaylists(
        PlatformAccountCredential credential,
        String operation
    ) {
        try {
            return tidalWebApiClient.getUserPlaylists(credential);
        } catch (RuntimeException exception) {
            throw providerOperationFailed(operation, exception);
        }
    }

    private List<TidalPlaylistTrack> getPlaylistTracks(
        PlatformAccountCredential credential,
        String playlistId
    ) {
        try {
            return tidalWebApiClient.getPlaylistTracks(credential, playlistId);
        } catch (RuntimeException exception) {
            throw providerOperationFailed("loading playlist tracks for %s".formatted(playlistId), exception);
        }
    }

    private PlatformProviderOperationException providerOperationFailed(
        String operation,
        RuntimeException exception
    ) {
        log.warn("TIDAL provider operation failed while {}: {}", operation, exception.getMessage());
        return new PlatformProviderOperationException("tidal", operation, exception);
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
