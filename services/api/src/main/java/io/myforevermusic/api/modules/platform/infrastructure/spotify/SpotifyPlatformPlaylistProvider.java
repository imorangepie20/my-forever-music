package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformPlaylistProvider;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyAudioFeaturesSnapshot;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistSummary;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyPlaylistTrack;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyUserProfile;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidatePlaylist;
import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportCatalogService.ImportCandidateTrack;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackSpotifyAudioFeatures;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SpotifyPlatformPlaylistProvider implements PlatformPlaylistProvider {

    private static final Logger log = LoggerFactory.getLogger(SpotifyPlatformPlaylistProvider.class);
    private static final List<Map.Entry<String, String>> GENRE_KEYWORDS = List.of(
        Map.entry("chill", "lo-fi beats"),
        Map.entry("focus", "ambient-pop"),
        Map.entry("night", "synth-pop"),
        Map.entry("drive", "electropop"),
        Map.entry("dream", "dream-pop"),
        Map.entry("ambient", "ambient-pop"),
        Map.entry("study", "downtempo"),
        Map.entry("dance", "dance-pop"),
        Map.entry("house", "deep house"),
        Map.entry("rock", "indie-rock"),
        Map.entry("rap", "hip-hop"),
        Map.entry("hip hop", "hip-hop"),
        Map.entry("jazz", "nu jazz")
    );
    private static final List<String> FALLBACK_GENRES = List.of(
        "synth-pop",
        "indietronica",
        "dream-pop",
        "ambient-pop",
        "electropop",
        "downtempo",
        "lo-fi beats",
        "indie-pop",
        "trip-hop",
        "deep house"
    );

    private final SpotifyWebApiClient spotifyWebApiClient;

    public SpotifyPlatformPlaylistProvider(SpotifyWebApiClient spotifyWebApiClient) {
        this.spotifyWebApiClient = spotifyWebApiClient;
    }

    @Override
    public boolean supports(String platformId, PlatformAccountCredential credential) {
        return "spotify".equals(platformId)
            && credential != null
            && credential.authorizationMode() != null
            && credential.authorizationMode().startsWith("spotify")
            && credential.accessToken() != null
            && !credential.accessToken().isBlank();
    }

    @Override
    public List<ImportCandidatePlaylist> listImportablePlaylists(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential
    ) {
        SpotifyUserProfile profile = spotifyWebApiClient.getCurrentUserProfile(credential);
        return getAccessiblePlaylists(profile, credential).stream()
            .map(playlist -> new ImportCandidatePlaylist(
                playlist.playlistId(),
                playlist.name(),
                "spotify",
                playlist.ownerDisplayName(),
                normalizeDescription(playlist.description()),
                playlist.coverImageUrl(),
                playlist.externalUrl(),
                playlist.spotifyUri(),
                playlist.trackCount(),
                List.of()
            ))
            .toList();
    }

    @Override
    public List<ImportCandidatePlaylist> loadPlaylistsForImport(
        AuthRegisteredAccount account,
        PlatformAccountCredential credential,
        List<String> externalPlaylistIds
    ) {
        SpotifyUserProfile profile = spotifyWebApiClient.getCurrentUserProfile(credential);
        Map<String, SpotifyPlaylistSummary> accessiblePlaylistsById = getAccessiblePlaylists(profile, credential).stream()
            .collect(Collectors.toMap(
                SpotifyPlaylistSummary::playlistId,
                playlist -> playlist,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        Map<String, List<SpotifyPlaylistTrack>> tracksByPlaylistId = new LinkedHashMap<>();
        for (String externalPlaylistId : externalPlaylistIds) {
            SpotifyPlaylistSummary playlist = Optional.ofNullable(accessiblePlaylistsById.get(externalPlaylistId))
                .orElseThrow(() -> new IllegalArgumentException(
                    "Spotify playlist is not available for import: %s".formatted(externalPlaylistId)
                ));
            tracksByPlaylistId.put(
                externalPlaylistId,
                spotifyWebApiClient.getPlaylistTracks(credential, playlist.playlistId())
            );
        }

        Map<String, SpotifyAudioFeaturesSnapshot> audioFeaturesByTrackId = resolveAudioFeatures(
            credential,
            tracksByPlaylistId.values().stream().flatMap(List::stream).toList()
        );
        Instant resolvedAt = Instant.now();

        return externalPlaylistIds.stream()
            .map(externalPlaylistId -> toImportCandidatePlaylist(
                accessiblePlaylistsById.get(externalPlaylistId),
                tracksByPlaylistId.getOrDefault(externalPlaylistId, List.of()),
                audioFeaturesByTrackId,
                resolvedAt
            ))
            .toList();
    }

    private List<SpotifyPlaylistSummary> getAccessiblePlaylists(
        SpotifyUserProfile profile,
        PlatformAccountCredential credential
    ) {
        return spotifyWebApiClient.getCurrentUserPlaylists(credential).stream()
            .filter(playlist -> isAccessible(profile, playlist))
            .toList();
    }

    private boolean isAccessible(SpotifyUserProfile profile, SpotifyPlaylistSummary playlist) {
        return profile.spotifyUserId() != null
            && profile.spotifyUserId().equals(playlist.ownerId())
            || playlist.collaborative();
    }

    private Map<String, SpotifyAudioFeaturesSnapshot> resolveAudioFeatures(
        PlatformAccountCredential credential,
        List<SpotifyPlaylistTrack> tracks
    ) {
        List<String> uniqueTrackIds = tracks.stream()
            .map(SpotifyPlaylistTrack::spotifyTrackId)
            .filter(trackId -> trackId != null && !trackId.isBlank())
            .distinct()
            .toList();

        if (uniqueTrackIds.isEmpty()) {
            return Map.of();
        }

        try {
            return spotifyWebApiClient.getTrackAudioFeatures(credential, uniqueTrackIds);
        } catch (RuntimeException exception) {
            log.warn("Spotify audio-features lookup failed: {}", exception.getMessage());
            throw new IllegalStateException(
                "Spotify audio features could not be resolved. No fallback audio feature snapshot will be generated.",
                exception
            );
        }
    }

    private ImportCandidatePlaylist toImportCandidatePlaylist(
        SpotifyPlaylistSummary playlist,
        List<SpotifyPlaylistTrack> tracks,
        Map<String, SpotifyAudioFeaturesSnapshot> audioFeaturesByTrackId,
        Instant resolvedAt
    ) {
        List<ImportCandidateTrack> importTracks = IntStream.range(0, tracks.size())
            .mapToObj(index -> {
                SpotifyPlaylistTrack track = tracks.get(index);
                return new ImportCandidateTrack(
                    track.spotifyTrackId(),
                    track.title(),
                    track.artistName(),
                    inferPrimaryGenre(playlist, track),
                    track.albumTitle(),
                    track.albumImageUrl(),
                    track.externalUrl(),
                    track.spotifyUri(),
                    track.previewUrl(),
                    index < 2,
                    resolveTrackAudioFeatures(track, audioFeaturesByTrackId.get(track.spotifyTrackId()), resolvedAt)
                );
            })
            .toList();

        return new ImportCandidatePlaylist(
            playlist.playlistId(),
            playlist.name(),
            "spotify",
            playlist.ownerDisplayName(),
            normalizeDescription(playlist.description()),
            playlist.coverImageUrl(),
            playlist.externalUrl(),
            playlist.spotifyUri(),
            importTracks.size(),
            importTracks
        );
    }

    private PmsTrackSpotifyAudioFeatures resolveTrackAudioFeatures(
        SpotifyPlaylistTrack track,
        SpotifyAudioFeaturesSnapshot snapshot,
        Instant resolvedAt
    ) {
        if (snapshot != null) {
            return new PmsTrackSpotifyAudioFeatures(
                track.spotifyTrackId(),
                "spotify_api",
                true,
                snapshot.analysisUrl(),
                snapshot.trackHref() == null ? track.trackHref() : snapshot.trackHref(),
                snapshot.spotifyUri() == null ? track.spotifyUri() : snapshot.spotifyUri(),
                snapshot.featureType() == null ? "audio_features" : snapshot.featureType(),
                snapshot.durationMs() == null ? track.durationMs() : snapshot.durationMs(),
                snapshot.musicalKey(),
                snapshot.mode(),
                snapshot.timeSignature(),
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

        throw new IllegalStateException(
            "Spotify audio features are missing for track %s (%s). Import is stopped so PMS does not store generated audio values."
                .formatted(track.spotifyTrackId(), track.title())
        );
    }

    private String inferPrimaryGenre(
        SpotifyPlaylistSummary playlist,
        SpotifyPlaylistTrack track
    ) {
        String haystack = "%s %s %s %s".formatted(
            playlist.name(),
            normalizeDescription(playlist.description()),
            track.title(),
            track.artistName()
        ).toLowerCase();

        for (Map.Entry<String, String> genreKeyword : GENRE_KEYWORDS) {
            if (haystack.contains(genreKeyword.getKey())) {
                return genreKeyword.getValue();
            }
        }

        return FALLBACK_GENRES.get(boundedInt(haystack, 14, 0, FALLBACK_GENRES.size() - 1));
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
            ? "Imported from the connected Spotify account."
            : description.trim();
    }

    private int boundedInt(String seed, int salt, int min, int max) {
        long normalized = Integer.toUnsignedLong((seed + "|" + salt).hashCode());
        return min + (int) (normalized % (max - min + 1));
    }

}
