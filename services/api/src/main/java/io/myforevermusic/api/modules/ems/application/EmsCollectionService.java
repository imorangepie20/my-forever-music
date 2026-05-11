package io.myforevermusic.api.modules.ems.application;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsTrackAudioFeatures;
import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformCredentialService;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsAudioFeaturesSnapshot;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsTrackLookupRequest;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistSummary;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifySearchResult;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistSummary;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalSearchResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmsCollectionService {

    private static final Logger log = LoggerFactory.getLogger(EmsCollectionService.class);
    private static final List<String> TIDAL_HOME_PAGE_SOURCE_IDS = List.of(
        "THE_HITS",
        "POPULAR_MIXES",
        "POPULAR_PLAYLISTS",
        "FROM_OUR_EDITORS"
    );

    private final SpotifyWebApiClient spotifyWebApiClient;
    private final TidalWebApiClient tidalWebApiClient;
    private final ReccoBeatsAudioFeaturesClient reccoBeatsAudioFeaturesClient;
    private final PlatformCredentialService platformCredentialService;
    private final AuthAccountStore authAccountStore;
    private final EmsCollectedPlaylistRepository playlistRepository;
    private final EmsCollectedTrackRepository trackRepository;
    private final EmsCollectedPlaylistTrackRepository playlistTrackRepository;

    public EmsCollectionService(
        SpotifyWebApiClient spotifyWebApiClient,
        TidalWebApiClient tidalWebApiClient,
        ReccoBeatsAudioFeaturesClient reccoBeatsAudioFeaturesClient,
        PlatformCredentialService platformCredentialService,
        AuthAccountStore authAccountStore,
        EmsCollectedPlaylistRepository playlistRepository,
        EmsCollectedTrackRepository trackRepository,
        EmsCollectedPlaylistTrackRepository playlistTrackRepository
    ) {
        this.spotifyWebApiClient = spotifyWebApiClient;
        this.tidalWebApiClient = tidalWebApiClient;
        this.reccoBeatsAudioFeaturesClient = reccoBeatsAudioFeaturesClient;
        this.platformCredentialService = platformCredentialService;
        this.authAccountStore = authAccountStore;
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    public EmsCollectionSearchPreviewResult previewSearch(String userId, String platformId, String query) {
        List<EmsCollectionSearchPlaylistPreview> playlists = new ArrayList<>();
        List<EmsCollectionSearchTrackPreview> tracks = new ArrayList<>();
        int totalPlaylistCount = 0;
        int totalTrackCount = 0;

        String requestedPlatformId = resolveSearchPlatformId(userId, platformId);
        PlatformAccountCredential credential = platformCredentialService
            .findUsableCredential(userId, requestedPlatformId)
            .orElse(null);
        if (credential == null) {
            throw new IllegalArgumentException(
                "Connect %s before searching EMS public playlists.".formatted(requestedPlatformId)
            );
        }

        SearchTotals totals;
        if ("spotify".equals(requestedPlatformId)) {
            totals = appendSpotifySearchResults(credential, query, playlists, tracks);
        } else if ("tidal".equals(requestedPlatformId)) {
            totals = appendTidalSearchResults(credential, query, playlists, tracks);
        } else {
            throw new IllegalArgumentException("Unsupported EMS search platform: %s".formatted(requestedPlatformId));
        }
        totalPlaylistCount += totals.playlistCount();
        totalTrackCount += totals.trackCount();

        return new EmsCollectionSearchPreviewResult(
            requestedPlatformId,
            query,
            playlists,
            tracks,
            totalPlaylistCount,
            totalTrackCount,
            Instant.now()
        );
    }

    public EmsCollectionSearchPlaylistTracksPreview getSearchPlaylistTracks(
        String userId,
        String platformId,
        String externalPlaylistId
    ) {
        PlatformAccountCredential credential = platformCredentialService
            .findUsableCredential(userId, platformId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Connect %s before loading EMS search playlist tracks.".formatted(platformId)
            ));

        if ("spotify".equals(platformId)) {
            List<EmsCollectionSearchTrackPreview> tracks = spotifyWebApiClient.getPlaylistTracks(credential, externalPlaylistId)
                .stream()
                .map(this::toSpotifySearchTrackPreview)
                .toList();
            return new EmsCollectionSearchPlaylistTracksPreview(platformId, externalPlaylistId, tracks, tracks.size(), Instant.now());
        }

        if ("tidal".equals(platformId)) {
            List<EmsCollectionSearchTrackPreview> tracks = tidalWebApiClient.getPlaylistTracks(credential, externalPlaylistId)
                .stream()
                .map(this::toTidalSearchTrackPreview)
                .toList();
            return new EmsCollectionSearchPlaylistTracksPreview(platformId, externalPlaylistId, tracks, tracks.size(), Instant.now());
        }

        throw new IllegalArgumentException("Unsupported EMS search platform: %s".formatted(platformId));
    }

    private String resolveSearchPlatformId(String userId, String platformId) {
        if (hasText(platformId) && !"all".equals(platformId)) {
            return platformId;
        }
        return authAccountStore.findByUserId(userId)
            .map(account -> account.preferredPlatformId())
            .filter(this::hasText)
            .orElseThrow(() -> new IllegalArgumentException(
                "A registered user with a preferred provider is required before searching EMS public playlists."
            ));
    }

    private SearchTotals appendSpotifySearchResults(
        PlatformAccountCredential credential,
        String query,
        List<EmsCollectionSearchPlaylistPreview> playlists,
        List<EmsCollectionSearchTrackPreview> tracks
    ) {
        log.info("EMS search preview: calling Spotify search only query='{}'", query);
        SpotifySearchResult<SpotifyPlaylistSummary> playlistResults =
            spotifyWebApiClient.searchPlaylists(credential, query);
        playlists.addAll(playlistResults.items().stream()
            .map(playlist -> new EmsCollectionSearchPlaylistPreview(
                playlist.playlistId(),
                playlist.name(),
                "spotify",
                playlist.ownerDisplayName(),
                playlist.description(),
                playlist.coverImageUrl(),
                playlist.externalUrl(),
                playlist.spotifyUri(),
                playlist.trackCount()
            ))
            .toList());
        SpotifySearchResult<SpotifyPlaylistTrack> trackResults =
            spotifyWebApiClient.searchTracks(credential, query);
        tracks.addAll(trackResults.items().stream()
            .map(this::toSpotifySearchTrackPreview)
            .toList());
        return new SearchTotals(
            Math.max(playlistResults.total(), playlistResults.items().size()),
            Math.max(trackResults.total(), trackResults.items().size())
        );
    }

    private SearchTotals appendTidalSearchResults(
        PlatformAccountCredential credential,
        String query,
        List<EmsCollectionSearchPlaylistPreview> playlists,
        List<EmsCollectionSearchTrackPreview> tracks
    ) {
        log.info("EMS search preview: calling TIDAL search only query='{}'", query);
        TidalSearchResult<TidalPlaylistSummary> playlistResults = tidalWebApiClient.searchPlaylistResults(credential, query);
        playlists.addAll(playlistResults.items().stream()
            .map(playlist -> new EmsCollectionSearchPlaylistPreview(
                playlist.playlistId(),
                playlist.name(),
                "tidal",
                "",
                playlist.description(),
                playlist.coverImageUrl(),
                playlist.externalUrl(),
                null,
                playlist.trackCount()
            ))
            .toList());
        TidalSearchResult<TidalPlaylistTrack> trackResults = tidalWebApiClient.searchTrackResults(credential, query);
        tracks.addAll(trackResults.items().stream()
            .map(this::toTidalSearchTrackPreview)
            .toList());
        return new SearchTotals(
            Math.max(playlistResults.total(), playlistResults.items().size()),
            Math.max(trackResults.total(), trackResults.items().size())
        );
    }

    private EmsCollectionSearchTrackPreview toSpotifySearchTrackPreview(SpotifyPlaylistTrack track) {
        return new EmsCollectionSearchTrackPreview(
            track.spotifyTrackId(),
            track.title(),
            track.artistName(),
            "spotify",
            track.isrc(),
            track.albumTitle(),
            track.albumImageUrl(),
            track.externalUrl(),
            track.spotifyUri(),
            track.previewUrl(),
            track.durationMs()
        );
    }

    private EmsCollectionSearchTrackPreview toTidalSearchTrackPreview(TidalPlaylistTrack track) {
        return new EmsCollectionSearchTrackPreview(
            track.tidalTrackId(),
            track.title(),
            track.artistName(),
            "tidal",
            track.isrc(),
            track.albumTitle(),
            track.albumImageUrl(),
            track.externalUrl(),
            track.tidalUri(),
            track.previewUrl(),
            track.durationMs() > 0 ? track.durationMs() : null
        );
    }

    @Transactional
    public EmsCollectionSearchResult collectPublicPlaylistPool(String userId, String platformId, String query, int limit) {
        return collectFromProvider(userId, platformId, query, limit, "public_pool");
    }

    private EmsCollectionSearchResult collectFromProvider(
        String userId,
        String platformId,
        String query,
        int limit,
        String collectionSource
    ) {
        PlatformAccountCredential credential = platformCredentialService
            .findUsableCredential(userId, platformId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Connect %s before collecting EMS public playlists.".formatted(platformId)
            ));

        Instant now = Instant.now();
        int collectedPlaylistCount = 0;
        int collectedTrackCount = 0;

        if ("spotify".equals(platformId)) {
            SpotifyPlaylistSource spotifySource = spotifyPlaylistSource(query);
            log.info(
                "EMS collection: calling Spotify playlist source={} source_id='{}' limit={}",
                collectionSource,
                query,
                limit
            );
            SpotifySearchResult<SpotifyPlaylistSummary> playlistResults =
                spotifySource.resolve(spotifyWebApiClient, credential, limit);

            for (SpotifyPlaylistSummary playlist : playlistResults.items()) {
                EmsCollectedPlaylistEntity playlistEntity = upsertPlaylist(playlist, collectionSource, query, now);
                collectedPlaylistCount++;

                try {
                    List<SpotifyPlaylistTrack> playlistTracks =
                        spotifyWebApiClient.getPlaylistTracks(credential, playlist.playlistId());
                    Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByTrackId =
                        resolveSpotifyAudioFeatures(playlistTracks);
                    Instant resolvedAt = Instant.now();
                    for (int i = 0; i < playlistTracks.size(); i++) {
                        SpotifyPlaylistTrack playlistTrack = playlistTracks.get(i);
                        EmsCollectedTrackEntity trackEntity = upsertTrack(
                            playlistTrack,
                            collectionSource,
                            now,
                            resolveSpotifyTrackAudioFeatures(
                                playlistTrack,
                                audioFeaturesByTrackId.get(playlistTrack.spotifyTrackId()),
                                resolvedAt
                            )
                        );
                        linkPlaylistTrack(playlistEntity, trackEntity, i);
                        collectedTrackCount++;
                    }
                } catch (Exception e) {
                    log.warn("EMS collection: could not fetch tracks for Spotify playlist {}: {}", playlist.playlistId(), e.getMessage());
                }
            }

            if (spotifySource.includeLooseTrackSearch()) {
                log.info("EMS collection: calling Spotify searchTracks source={} query='{}' limit={}", collectionSource, query, limit);
                SpotifySearchResult<SpotifyPlaylistTrack> trackResults =
                    spotifyWebApiClient.searchTracks(credential, query, limit);

                Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByTrackId =
                    resolveSpotifyAudioFeatures(trackResults.items());
                Instant resolvedAt = Instant.now();
                for (SpotifyPlaylistTrack track : trackResults.items()) {
                    upsertTrack(
                        track,
                        collectionSource,
                        now,
                        resolveSpotifyTrackAudioFeatures(track, audioFeaturesByTrackId.get(track.spotifyTrackId()), resolvedAt)
                    );
                    collectedTrackCount++;
                }
            }
        } else if ("tidal".equals(platformId)) {
            boolean homePageSource = isTidalHomePageSource(query);
            log.info(
                "EMS collection: calling TIDAL playlist source={} source_id='{}' limit={}",
                collectionSource,
                query,
                limit
            );
            List<TidalPlaylistSummary> playlistResults = homePageSource
                ? tidalWebApiClient.getHomePagePlaylists(credential, query, limit)
                : tidalWebApiClient.searchPlaylists(credential, query, limit);

            for (TidalPlaylistSummary playlist : playlistResults) {
                EmsCollectedPlaylistEntity playlistEntity = upsertPlaylistFromTidal(playlist, collectionSource, query, now);
                collectedPlaylistCount++;

                try {
                    List<TidalPlaylistTrack> playlistTracks =
                        tidalWebApiClient.getPlaylistTracks(credential, playlist.playlistId());
                    Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByTrackId =
                        resolveTidalAudioFeatures(playlistTracks);
                    Instant resolvedAt = Instant.now();
                    for (int i = 0; i < playlistTracks.size(); i++) {
                        TidalPlaylistTrack playlistTrack = playlistTracks.get(i);
                        EmsCollectedTrackEntity trackEntity = upsertTrackFromTidal(
                            playlistTrack,
                            collectionSource,
                            now,
                            resolveTidalTrackAudioFeatures(
                                playlistTrack,
                                audioFeaturesByTrackId.get(playlistTrack.tidalTrackId()),
                                resolvedAt
                            )
                        );
                        linkPlaylistTrack(playlistEntity, trackEntity, i);
                        collectedTrackCount++;
                    }
                } catch (Exception e) {
                    log.warn("EMS collection: could not fetch tracks for TIDAL playlist {}: {}", playlist.playlistId(), e.getMessage());
                }
            }

            if (!homePageSource) {
                log.info("EMS collection: calling TIDAL searchTracks source={} query='{}' limit={}", collectionSource, query, limit);
                List<TidalPlaylistTrack> trackResults = tidalWebApiClient.searchTracks(credential, query, limit);

                Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByTrackId =
                    resolveTidalAudioFeatures(trackResults);
                Instant resolvedAt = Instant.now();
                for (TidalPlaylistTrack track : trackResults) {
                    upsertTrackFromTidal(
                        track,
                        collectionSource,
                        now,
                        resolveTidalTrackAudioFeatures(track, audioFeaturesByTrackId.get(track.tidalTrackId()), resolvedAt)
                    );
                    collectedTrackCount++;
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported EMS collection platform: %s".formatted(platformId));
        }

        log.info(
            "EMS collection source={} collected {} playlists and {} tracks for platform={} query='{}'",
            collectionSource,
            collectedPlaylistCount,
            collectedTrackCount,
            platformId,
            query
        );

        return new EmsCollectionSearchResult(
            platformId, query, collectedPlaylistCount, collectedTrackCount, now
        );
    }

    public List<EmsCollectedPlaylistEntity> getCollectedPlaylists(String platformId, int limit, boolean randomize) {
        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        if (randomize) {
            return playlistRepository.findRandomBySourcePlatform(platformId, clampedLimit);
        }
        return playlistRepository.findBySourcePlatformOrderByCollectedAtDesc(platformId, clampedLimit);
    }

    public EmsCollectedPlaylistEntity getCollectedPlaylist(Long playlistId) {
        return playlistRepository.findById(playlistId)
            .orElseThrow(() -> new ApiResourceNotFoundException(
                "EMS collected playlist was not found: %s".formatted(playlistId)
            ));
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

    public EmsAudioFeatureCoverage getAudioFeatureCoverage(Long playlistId) {
        long linkedTrackCount = playlistTrackRepository.countTracksByPlaylistId(playlistId);
        long filledTrackCount = playlistTrackRepository.countAudioFeatureFilledTracksByPlaylistId(playlistId);
        long pendingTrackCount = Math.max(0, linkedTrackCount - filledTrackCount);
        double coverageRatio = linkedTrackCount == 0 ? 0.0 : (double) filledTrackCount / linkedTrackCount;
        return new EmsAudioFeatureCoverage(linkedTrackCount, filledTrackCount, pendingTrackCount, coverageRatio);
    }

    @Transactional
    public EmsAudioFeatureBackfillResult backfillAudioFeaturesForPlaylist(Long playlistId) {
        EmsCollectedPlaylistEntity playlist = getCollectedPlaylist(playlistId);
        List<EmsCollectedTrackEntity> tracks = getTracksForPlaylist(playlistId);
        long filledBefore = tracks.stream().filter(this::hasFilledAudioFeatures).count();
        List<EmsCollectedTrackEntity> pendingTracks = tracks.stream()
            .filter(track -> !hasFilledAudioFeatures(track))
            .toList();

        Map<String, ReccoBeatsAudioFeaturesSnapshot> snapshots = resolveAudioFeaturesForBackfill(
            playlist.getSourcePlatform(),
            pendingTracks
        );

        Instant resolvedAt = Instant.now();
        int matchedSnapshotCount = 0;
        int updatedTrackCount = 0;
        int newlyFilledTrackCount = 0;
        List<EmsCollectedTrackEntity> updatedTracks = new java.util.ArrayList<>();
        for (EmsCollectedTrackEntity track : pendingTracks) {
            ReccoBeatsAudioFeaturesSnapshot snapshot = snapshots.get(track.getExternalTrackId());
            if (snapshot == null) {
                continue;
            }

            matchedSnapshotCount++;
            EmsTrackAudioFeatures audioFeatures = toBackfilledAudioFeatures(track, snapshot, resolvedAt);
            track.applyAudioFeatures(audioFeatures);
            updatedTrackCount++;
            updatedTracks.add(track);
            if (audioFeatures.isAudioFeaturesFilled()) {
                newlyFilledTrackCount++;
            }
        }

        if (updatedTrackCount > 0) {
            trackRepository.saveAll(updatedTracks);
        }

        long filledAfter = filledBefore + newlyFilledTrackCount;
        long pendingAfter = Math.max(0, tracks.size() - filledAfter);
        double coverageRatio = tracks.isEmpty() ? 0.0 : (double) filledAfter / tracks.size();
        return new EmsAudioFeatureBackfillResult(
            playlist.getId(),
            playlist.getTitle(),
            playlist.getSourcePlatform(),
            tracks.size(),
            filledBefore,
            pendingTracks.size(),
            eligibleBackfillTrackCount(playlist.getSourcePlatform(), pendingTracks),
            missingIsrcTrackCount(playlist.getSourcePlatform(), pendingTracks),
            matchedSnapshotCount,
            updatedTrackCount,
            newlyFilledTrackCount,
            filledAfter,
            pendingAfter,
            coverageRatio,
            resolvedAt
        );
    }

    private Map<String, ReccoBeatsAudioFeaturesSnapshot> resolveAudioFeaturesForBackfill(
        String sourcePlatform,
        List<EmsCollectedTrackEntity> tracks
    ) {
        if ("spotify".equals(sourcePlatform)) {
            List<String> spotifyTrackIds = tracks.stream()
                .map(EmsCollectedTrackEntity::getExternalTrackId)
                .filter(this::hasText)
                .distinct()
                .toList();
            if (spotifyTrackIds.isEmpty()) {
                return Map.of();
            }
            return reccoBeatsAudioFeaturesClient.getAudioFeaturesForSpotifyTrackIds(spotifyTrackIds);
        }

        if ("tidal".equals(sourcePlatform)) {
            List<ReccoBeatsTrackLookupRequest> lookupRequests = tracks.stream()
                .filter(track -> hasText(track.getExternalTrackId()) && hasText(track.getIsrc()))
                .map(track -> new ReccoBeatsTrackLookupRequest(
                    track.getExternalTrackId(),
                    track.getTitle(),
                    track.getArtistName(),
                    track.getDurationMs(),
                    track.getIsrc()
                ))
                .toList();
            if (lookupRequests.isEmpty()) {
                return Map.of();
            }
            return reccoBeatsAudioFeaturesClient.getAudioFeaturesForExternalTracksByIsrc(lookupRequests);
        }

        return Map.of();
    }

    private int eligibleBackfillTrackCount(String sourcePlatform, List<EmsCollectedTrackEntity> tracks) {
        if ("spotify".equals(sourcePlatform)) {
            return (int) tracks.stream()
                .map(EmsCollectedTrackEntity::getExternalTrackId)
                .filter(this::hasText)
                .distinct()
                .count();
        }
        if ("tidal".equals(sourcePlatform)) {
            return (int) tracks.stream()
                .filter(track -> hasText(track.getExternalTrackId()) && hasText(track.getIsrc()))
                .count();
        }
        return 0;
    }

    private int missingIsrcTrackCount(String sourcePlatform, List<EmsCollectedTrackEntity> tracks) {
        if (!"tidal".equals(sourcePlatform)) {
            return 0;
        }
        return (int) tracks.stream()
            .filter(track -> hasText(track.getExternalTrackId()) && !hasText(track.getIsrc()))
            .count();
    }

    private EmsTrackAudioFeatures toBackfilledAudioFeatures(
        EmsCollectedTrackEntity track,
        ReccoBeatsAudioFeaturesSnapshot snapshot,
        Instant resolvedAt
    ) {
        Integer durationMs = track.getDurationMs();
        String audioFeatureSource = "tidal".equals(track.getSourcePlatform())
            ? "reccobeats_isrc_match"
            : "reccobeats_lookup";
        return new EmsTrackAudioFeatures(
            firstNonBlank(snapshot.spotifyTrackId(), track.getExternalTrackId()),
            audioFeatureSource,
            hasCompleteAudioFeatures(snapshot, durationMs),
            null,
            firstNonBlank(snapshot.spotifyTrackHref(), track.getPlatformExternalUrl()),
            firstNonBlank(buildSpotifyUri(snapshot.spotifyTrackId()), track.getSpotifyUri()),
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

    private boolean hasFilledAudioFeatures(EmsCollectedTrackEntity track) {
        return track.getAudioFeatures() != null && track.getAudioFeatures().isAudioFeaturesFilled();
    }

    private Map<String, ReccoBeatsAudioFeaturesSnapshot> resolveSpotifyAudioFeatures(List<SpotifyPlaylistTrack> tracks) {
        List<String> spotifyTrackIds = tracks.stream()
            .map(SpotifyPlaylistTrack::spotifyTrackId)
            .filter(this::hasText)
            .distinct()
            .toList();

        if (spotifyTrackIds.isEmpty()) {
            return Map.of();
        }

        try {
            return reccoBeatsAudioFeaturesClient.getAudioFeaturesForSpotifyTrackIds(spotifyTrackIds);
        } catch (RuntimeException exception) {
            log.warn(
                "EMS collection: ReccoBeats Spotify audio-features lookup failed: {}. Tracks will be stored with unavailable audio features.",
                exception.getMessage()
            );
            return Map.of();
        }
    }

    private Map<String, ReccoBeatsAudioFeaturesSnapshot> resolveTidalAudioFeatures(List<TidalPlaylistTrack> tracks) {
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
                "EMS collection: ReccoBeats TIDAL ISRC audio-features lookup failed: {}. Tracks will be stored with unavailable audio features.",
                exception.getMessage()
            );
            return Map.of();
        }
    }

    private EmsTrackAudioFeatures resolveSpotifyTrackAudioFeatures(
        SpotifyPlaylistTrack track,
        ReccoBeatsAudioFeaturesSnapshot snapshot,
        Instant resolvedAt
    ) {
        if (snapshot != null) {
            return new EmsTrackAudioFeatures(
                track.spotifyTrackId(),
                "reccobeats_lookup",
                hasCompleteAudioFeatures(snapshot, track.durationMs()),
                null,
                snapshot.spotifyTrackHref() == null ? track.externalUrl() : snapshot.spotifyTrackHref(),
                track.spotifyUri(),
                "audio_features",
                track.durationMs(),
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

        return unavailableAudioFeatures(track.spotifyTrackId(), track.externalUrl(), track.spotifyUri(), track.durationMs(), resolvedAt);
    }

    private EmsTrackAudioFeatures resolveTidalTrackAudioFeatures(
        TidalPlaylistTrack track,
        ReccoBeatsAudioFeaturesSnapshot snapshot,
        Instant resolvedAt
    ) {
        Integer durationMs = track.durationMs() > 0 ? track.durationMs() : null;
        if (snapshot != null) {
            return new EmsTrackAudioFeatures(
                snapshot.spotifyTrackId(),
                "reccobeats_isrc_match",
                hasCompleteAudioFeatures(snapshot, durationMs),
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

        return unavailableAudioFeatures(null, null, null, durationMs, resolvedAt);
    }

    private EmsTrackAudioFeatures unavailableAudioFeatures(
        String audioFeatureTrackId,
        String trackHref,
        String audioTrackUri,
        Integer durationMs,
        Instant resolvedAt
    ) {
        return new EmsTrackAudioFeatures(
            audioFeatureTrackId,
            "unavailable",
            false,
            null,
            trackHref,
            audioTrackUri,
            "audio_features",
            durationMs,
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
            resolvedAt
        );
    }

    private boolean hasCompleteAudioFeatures(ReccoBeatsAudioFeaturesSnapshot snapshot, Integer durationMs) {
        return snapshot != null
            && durationMs != null
            && snapshot.musicalKey() != null
            && snapshot.mode() != null
            && snapshot.acousticness() != null
            && snapshot.danceability() != null
            && snapshot.energy() != null
            && snapshot.instrumentalness() != null
            && snapshot.liveness() != null
            && snapshot.loudness() != null
            && snapshot.speechiness() != null
            && snapshot.tempo() != null
            && snapshot.valence() != null;
    }

    private String buildSpotifyUri(String spotifyTrackId) {
        return hasText(spotifyTrackId) ? "spotify:track:%s".formatted(spotifyTrackId) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean isTidalHomePageSource(String value) {
        return value != null && TIDAL_HOME_PAGE_SOURCE_IDS.contains(value.trim());
    }

    private SpotifyPlaylistSource spotifyPlaylistSource(String sourceId) {
        if (sourceId != null) {
            String normalized = sourceId.trim();
            if ("featured-playlists".equals(normalized) || "spotify:featured-playlists".equals(normalized)) {
                return new SpotifyPlaylistSource(
                    false,
                    (client, credential, limit) -> client.getFeaturedPlaylists(credential, limit)
                );
            }
            String categoryPrefix = "category:";
            String namespacedCategoryPrefix = "spotify:category:";
            if (normalized.startsWith(categoryPrefix) && normalized.length() > categoryPrefix.length()) {
                String categoryId = normalized.substring(categoryPrefix.length());
                return new SpotifyPlaylistSource(
                    false,
                    (client, credential, limit) -> client.getCategoryPlaylists(credential, categoryId, limit)
                );
            }
            if (normalized.startsWith(namespacedCategoryPrefix) && normalized.length() > namespacedCategoryPrefix.length()) {
                String categoryId = normalized.substring(namespacedCategoryPrefix.length());
                return new SpotifyPlaylistSource(
                    false,
                    (client, credential, limit) -> client.getCategoryPlaylists(credential, categoryId, limit)
                );
            }
        }
        return new SpotifyPlaylistSource(
            true,
            (client, credential, limit) -> client.searchPlaylists(credential, sourceId, limit)
        );
    }

    private EmsCollectedPlaylistEntity upsertPlaylist(
        SpotifyPlaylistSummary playlist, String source, String query, Instant now
    ) {
        return playlistRepository.findBySourcePlatformAndExternalPlaylistId("spotify", playlist.playlistId())
            .map(existing -> {
                existing.applyCollectedMetadata(
                    playlist.name(),
                    playlist.ownerDisplayName(),
                    playlist.description() != null ? playlist.description() : "",
                    playlist.coverImageUrl(),
                    playlist.externalUrl(),
                    playlist.spotifyUri(),
                    playlist.trackCount(),
                    source,
                    query,
                    now
                );
                return existing;
            })
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
        SpotifyPlaylistTrack track, String source, Instant now, EmsTrackAudioFeatures audioFeatures
    ) {
        return upsertCollectedTrack(
            track.spotifyTrackId(),
            track.title(),
            track.artistName(),
            "spotify",
            null,
            track.albumTitle(),
            track.albumImageUrl(),
            track.externalUrl(),
            track.spotifyUri(),
            track.previewUrl(),
            track.durationMs(),
            source,
            now,
            audioFeatures
        );
    }

    private EmsCollectedPlaylistEntity upsertPlaylistFromTidal(
        TidalPlaylistSummary playlist, String source, String query, Instant now
    ) {
        return playlistRepository.findBySourcePlatformAndExternalPlaylistId("tidal", playlist.playlistId())
            .map(existing -> {
                existing.applyCollectedMetadata(
                    playlist.name(),
                    "",
                    playlist.description() != null ? playlist.description() : "",
                    playlist.coverImageUrl(),
                    playlist.externalUrl(),
                    null,
                    playlist.trackCount(),
                    source,
                    query,
                    now
                );
                return existing;
            })
            .orElseGet(() -> playlistRepository.save(new EmsCollectedPlaylistEntity(
                playlist.playlistId(),
                playlist.name(),
                "tidal",
                "",
                playlist.description() != null ? playlist.description() : "",
                playlist.coverImageUrl(),
                playlist.externalUrl(),
                null,
                playlist.trackCount(),
                source,
                query,
                now
            )));
    }

    private EmsCollectedTrackEntity upsertTrackFromTidal(
        TidalPlaylistTrack track, String source, Instant now, EmsTrackAudioFeatures audioFeatures
    ) {
        return upsertCollectedTrack(
            track.tidalTrackId(),
            track.title(),
            track.artistName(),
            "tidal",
            track.isrc(),
            track.albumTitle(),
            track.albumImageUrl(),
            track.externalUrl(),
            track.tidalUri(),
            track.previewUrl(),
            track.durationMs(),
            source,
            now,
            audioFeatures
        );
    }

    private EmsCollectedTrackEntity upsertCollectedTrack(
        String externalTrackId,
        String title,
        String artistName,
        String sourcePlatform,
        String isrc,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        Integer durationMs,
        String collectionSource,
        Instant collectedAt,
        EmsTrackAudioFeatures audioFeatures
    ) {
        trackRepository.upsertByExternalTrack(
            externalTrackId,
            title,
            artistName,
            sourcePlatform,
            isrc,
            albumTitle,
            albumImageUrl,
            platformExternalUrl,
            platformUri,
            previewUrl,
            durationMs,
            collectionSource,
            collectedAt,
            audioFeatures.getAudioFeatureTrackId(),
            audioFeatures.getAudioFeatureSource(),
            audioFeatures.isAudioFeaturesFilled(),
            audioFeatures.getAnalysisUrl(),
            audioFeatures.getTrackHref(),
            audioFeatures.getAudioTrackUri(),
            audioFeatures.getFeatureType(),
            audioFeatures.getDurationMs(),
            audioFeatures.getMusicalKey(),
            audioFeatures.getMode(),
            audioFeatures.getTimeSignature(),
            audioFeatures.getAcousticness(),
            audioFeatures.getDanceability(),
            audioFeatures.getEnergy(),
            audioFeatures.getInstrumentalness(),
            audioFeatures.getLiveness(),
            audioFeatures.getLoudness(),
            audioFeatures.getSpeechiness(),
            audioFeatures.getTempo(),
            audioFeatures.getValence(),
            audioFeatures.getResolvedAt()
        );
        return trackRepository.findBySourcePlatformAndExternalTrackId(sourcePlatform, externalTrackId)
            .orElseThrow(() -> new IllegalStateException(
                "EMS collected track upsert did not return a row: %s:%s".formatted(sourcePlatform, externalTrackId)
            ));
    }

    private void linkPlaylistTrack(EmsCollectedPlaylistEntity playlist, EmsCollectedTrackEntity track, int order) {
        playlistTrackRepository.upsertPlaylistTrackLink(playlist.getId(), track.getId(), order);
    }

    public record EmsCollectionSearchResult(
        String platformId,
        String query,
        int collectedPlaylistCount,
        int collectedTrackCount,
        Instant collectedAt
    ) {}

    public record EmsCollectionSearchPreviewResult(
        String platformId,
        String query,
        List<EmsCollectionSearchPlaylistPreview> playlists,
        List<EmsCollectionSearchTrackPreview> tracks,
        int resultPlaylistCount,
        int resultTrackCount,
        Instant searchedAt
    ) {}

    public record EmsCollectionSearchPlaylistPreview(
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        String curator,
        String description,
        String coverImageUrl,
        String platformExternalUrl,
        String platformUri,
        int trackCount
    ) {}

    public record EmsCollectionSearchTrackPreview(
        String externalTrackId,
        String title,
        String artistName,
        String sourcePlatform,
        String isrc,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        Integer durationMs
    ) {}

    public record EmsCollectionSearchPlaylistTracksPreview(
        String platformId,
        String externalPlaylistId,
        List<EmsCollectionSearchTrackPreview> tracks,
        int trackCount,
        Instant searchedAt
    ) {}

    public record EmsAudioFeatureCoverage(
        long trackCount,
        long filledTrackCount,
        long pendingTrackCount,
        double coverageRatio
    ) {}

    public record EmsAudioFeatureBackfillResult(
        Long playlistId,
        String playlistTitle,
        String sourcePlatform,
        long trackCount,
        long filledTrackCountBefore,
        long pendingTrackCountBefore,
        int eligibleTrackCount,
        int missingIsrcTrackCount,
        int matchedSnapshotCount,
        int updatedTrackCount,
        int newlyFilledTrackCount,
        long filledTrackCountAfter,
        long pendingTrackCountAfter,
        double coverageRatioAfter,
        Instant backfilledAt
    ) {}

    private record SpotifyPlaylistSource(
        boolean includeLooseTrackSearch,
        SpotifyPlaylistSourceResolver resolver
    ) {
        SpotifySearchResult<SpotifyPlaylistSummary> resolve(
            SpotifyWebApiClient client,
            PlatformAccountCredential credential,
            int limit
        ) {
            return resolver.resolve(client, credential, limit);
        }
    }

    @FunctionalInterface
    private interface SpotifyPlaylistSourceResolver {
        SpotifySearchResult<SpotifyPlaylistSummary> resolve(
            SpotifyWebApiClient client,
            PlatformAccountCredential credential,
            int limit
        );
    }

    private record SearchTotals(int playlistCount, int trackCount) {}
}
