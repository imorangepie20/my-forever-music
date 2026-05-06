package io.myforevermusic.api.modules.ems.application;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistSummary;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifySearchResult;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmsCollectionService {

    private static final Logger log = LoggerFactory.getLogger(EmsCollectionService.class);

    private final SpotifyWebApiClient spotifyWebApiClient;
    private final PlatformCredentialService platformCredentialService;
    private final EmsCollectedPlaylistRepository playlistRepository;
    private final EmsCollectedTrackRepository trackRepository;
    private final EmsCollectedPlaylistTrackRepository playlistTrackRepository;

    public EmsCollectionService(
        SpotifyWebApiClient spotifyWebApiClient,
        PlatformCredentialService platformCredentialService,
        EmsCollectedPlaylistRepository playlistRepository,
        EmsCollectedTrackRepository trackRepository,
        EmsCollectedPlaylistTrackRepository playlistTrackRepository
    ) {
        this.spotifyWebApiClient = spotifyWebApiClient;
        this.platformCredentialService = platformCredentialService;
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    @Transactional
    public EmsCollectionSearchResult collectFromSearch(String userId, String platformId, String query, int limit) {
        PlatformAccountCredential credential = platformCredentialService
            .findUsableCredential(userId, platformId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Connect %s before searching external music.".formatted(platformId)
            ));

        Instant now = Instant.now();
        int collectedPlaylistCount = 0;
        int collectedTrackCount = 0;

        if ("spotify".equals(platformId)) {
            log.info("EMS search: calling searchPlaylists for query='{}' limit={}", query, limit);
            SpotifySearchResult<SpotifyPlaylistSummary> playlistResults =
                spotifyWebApiClient.searchPlaylists(credential, query, limit);

            for (SpotifyPlaylistSummary playlist : playlistResults.items()) {
                EmsCollectedPlaylistEntity playlistEntity = upsertPlaylist(playlist, "search", query, now);
                collectedPlaylistCount++;

                try {
                    List<SpotifyPlaylistTrack> playlistTracks =
                        spotifyWebApiClient.getPlaylistTracks(credential, playlist.playlistId());
                    for (int i = 0; i < playlistTracks.size(); i++) {
                        EmsCollectedTrackEntity trackEntity = upsertTrack(playlistTracks.get(i), "search", now);
                        linkPlaylistTrack(playlistEntity, trackEntity, i);
                        collectedTrackCount++;
                    }
                } catch (Exception e) {
                    log.warn("EMS search: could not fetch tracks for playlist {}: {}", playlist.playlistId(), e.getMessage());
                }
            }

            log.info("EMS search: calling searchTracks for query='{}' limit={}", query, limit);
            SpotifySearchResult<SpotifyPlaylistTrack> trackResults =
                spotifyWebApiClient.searchTracks(credential, query, limit);

            for (SpotifyPlaylistTrack track : trackResults.items()) {
                upsertTrack(track, "search", now);
                collectedTrackCount++;
            }
        }

        log.info("EMS search collected {} playlists and {} tracks for query '{}'", collectedPlaylistCount, collectedTrackCount, query);

        return new EmsCollectionSearchResult(
            platformId, query, collectedPlaylistCount, collectedTrackCount, now
        );
    }

    public List<EmsCollectedPlaylistEntity> getCollectedPlaylists(String platformId) {
        return playlistRepository.findBySourcePlatformOrderByCollectedAtDesc(platformId);
    }

    public List<EmsCollectedTrackEntity> getCollectedTracks(String platformId) {
        return trackRepository.findBySourcePlatformOrderByCollectedAtDesc(platformId);
    }

    public List<EmsCollectedTrackEntity> getTracksForPlaylist(Long playlistId) {
        return playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(playlistId)
            .stream()
            .map(EmsCollectedPlaylistTrackEntity::getTrack)
            .toList();
    }

    private EmsCollectedPlaylistEntity upsertPlaylist(
        SpotifyPlaylistSummary playlist, String source, String query, Instant now
    ) {
        return playlistRepository.findBySourcePlatformAndExternalPlaylistId("spotify", playlist.playlistId())
            .orElseGet(() -> playlistRepository.save(new EmsCollectedPlaylistEntity(
                playlist.playlistId(),
                playlist.name(),
                "spotify",
                playlist.ownerDisplayName(),
                playlist.description() != null ? playlist.description() : "",
                playlist.coverImageUrl(),
                playlist.externalUrl(),
                playlist.spotifyUri(),
                playlist.trackCount(),
                source,
                query,
                now
            )));
    }

    private EmsCollectedTrackEntity upsertTrack(
        SpotifyPlaylistTrack track, String source, Instant now
    ) {
        return trackRepository.findBySourcePlatformAndExternalTrackId("spotify", track.spotifyTrackId())
            .orElseGet(() -> trackRepository.save(new EmsCollectedTrackEntity(
                track.spotifyTrackId(),
                track.title(),
                track.artistName(),
                "spotify",
                track.albumTitle(),
                track.albumImageUrl(),
                track.externalUrl(),
                track.spotifyUri(),
                track.previewUrl(),
                track.durationMs(),
                source,
                now
            )));
    }

    private void linkPlaylistTrack(EmsCollectedPlaylistEntity playlist, EmsCollectedTrackEntity track, int order) {
        boolean exists = playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(playlist.getId())
            .stream()
            .anyMatch(pt -> pt.getTrack().getId().equals(track.getId()));

        if (!exists) {
            playlistTrackRepository.save(new EmsCollectedPlaylistTrackEntity(playlist, track, order));
        }
    }

    public record EmsCollectionSearchResult(
        String platformId,
        String query,
        int collectedPlaylistCount,
        int collectedTrackCount,
        Instant collectedAt
    ) {}
}
