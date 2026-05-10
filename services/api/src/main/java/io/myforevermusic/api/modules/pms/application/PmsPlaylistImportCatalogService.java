package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("sandbox")
public class PmsPlaylistImportCatalogService {

    private static final Instant RESOLVED_AT = Instant.parse("2026-05-03T00:00:00Z");

    private static final Map<String, List<ImportCandidatePlaylist>> CATALOG = Map.of(
        "spotify",
        List.of(
            playlist(
                "spotify-liked-night-drive",
                "Liked Songs Night Drive",
                "spotify",
                "spotify-library",
                "High replay late-night mix from the connected Spotify account.",
                track(
                    "sp-liked-001",
                    "Midnight Receiver",
                    "Neon Bloom",
                    "synth-pop",
                    true,
                    audioFeatures("sp-track-midnight-receiver", "spotify_api", 218000, 1, 1, 4, 0.19, 0.74, 0.78, 0.02, 0.11, -7.8, 0.05, 116.2, 0.67)
                ),
                track(
                    "sp-liked-002",
                    "Static Heartline",
                    "Glass Harbor",
                    "dream-pop",
                    true,
                    audioFeatures("sp-track-static-heartline", "spotify_api", 241000, 6, 1, 4, 0.31, 0.63, 0.61, 0.08, 0.14, -9.2, 0.04, 104.8, 0.58)
                ),
                track(
                    "sp-liked-003",
                    "Signal in Rain",
                    "Aerial Youth",
                    "indietronica",
                    false,
                    audioFeatures("sp-track-signal-in-rain", "spotify_api", 233000, 9, 0, 4, 0.24, 0.69, 0.71, 0.03, 0.17, -8.5, 0.06, 122.1, 0.49)
                )
            ),
            playlist(
                "spotify-chill-focus-stack",
                "Chill Focus Stack",
                "spotify",
                "spotify-editorial",
                "Focus-oriented playlist with stable energy and low skip risk.",
                track(
                    "sp-focus-001",
                    "Paper Satellites",
                    "North Static",
                    "lo-fi beats",
                    true,
                    audioFeatures("sp-track-paper-satellites", "spotify_match", 197000, 2, 1, 4, 0.48, 0.58, 0.46, 0.15, 0.09, -11.4, 0.07, 89.4, 0.41)
                ),
                track(
                    "sp-focus-002",
                    "Quiet Index",
                    "Mono District",
                    "ambient-pop",
                    true,
                    audioFeatures("sp-track-quiet-index", "spotify_match", 221000, 11, 1, 4, 0.52, 0.55, 0.49, 0.21, 0.12, -10.7, 0.05, 93.2, 0.38)
                ),
                track(
                    "sp-focus-003",
                    "Late Office Sky",
                    "Pacific Ledger",
                    "downtempo",
                    false,
                    audioFeatures("sp-track-late-office-sky", "spotify_match", 248000, 4, 1, 4, 0.43, 0.51, 0.44, 0.26, 0.08, -12.1, 0.04, 87.1, 0.35)
                )
            )
        ),
        "apple-music",
        List.of(
            playlist(
                "apple-library-soft-bloom",
                "Soft Bloom Archive",
                "apple-music",
                "apple-library",
                "Saved Apple Music playlist with gentle mood transitions.",
                track(
                    "am-soft-001",
                    "Velvet Window",
                    "Sunday Harbor",
                    "dream-pop",
                    true,
                    audioFeatures("sp-track-velvet-window", "spotify_match", 229000, 8, 1, 4, 0.29, 0.61, 0.57, 0.06, 0.13, -9.7, 0.04, 101.5, 0.63)
                ),
                track(
                    "am-soft-002",
                    "Cloud Manual",
                    "Luma Fields",
                    "indie-folk",
                    true,
                    audioFeatures("sp-track-cloud-manual", "spotify_match", 215000, 3, 1, 4, 0.67, 0.49, 0.38, 0.01, 0.1, -12.8, 0.05, 82.7, 0.56)
                ),
                track(
                    "am-soft-003",
                    "Harborline",
                    "Delta Archive",
                    "ambient-pop",
                    false,
                    audioFeatures("sp-track-harborline", "fallback_generated", 244000, 10, 1, 4, 0.44, 0.53, 0.42, 0.18, 0.07, -11.9, 0.03, 90.9, 0.46)
                )
            ),
            playlist(
                "apple-motion-replay",
                "Motion Replay",
                "apple-music",
                "apple-editorial",
                "Brighter replay cluster suitable for upbeat EMS expansion.",
                track(
                    "am-motion-001",
                    "Silver Relay",
                    "Atlas Choir",
                    "synth-pop",
                    true,
                    audioFeatures("sp-track-silver-relay", "spotify_match", 207000, 1, 1, 4, 0.16, 0.77, 0.8, 0.0, 0.1, -7.2, 0.05, 121.8, 0.72)
                ),
                track(
                    "am-motion-002",
                    "Glow Operator",
                    "Circuit Museum",
                    "electropop",
                    true,
                    audioFeatures("sp-track-glow-operator", "spotify_match", 213000, 5, 1, 4, 0.14, 0.75, 0.76, 0.01, 0.16, -7.6, 0.04, 118.6, 0.69)
                ),
                track(
                    "am-motion-003",
                    "Tape Horizon",
                    "Petal Circuit",
                    "indietronica",
                    false,
                    audioFeatures("sp-track-tape-horizon", "fallback_generated", 236000, 7, 0, 4, 0.2, 0.68, 0.72, 0.09, 0.12, -8.8, 0.05, 114.3, 0.52)
                )
            )
        ),
        "tidal",
        List.of(
            playlist(
                "tidal-hi-fi-noir",
                "Hi-Fi Noir Rotation",
                "tidal",
                "tidal-library",
                "Warm low-end and moody pacing from the connected TIDAL account.",
                track(
                    "td-noir-001",
                    "Noir Transit",
                    "Cinder Avenue",
                    "trip-hop",
                    true,
                    audioFeatures("sp-track-noir-transit", "spotify_match", 252000, 2, 0, 4, 0.37, 0.59, 0.52, 0.11, 0.18, -10.6, 0.07, 95.3, 0.34)
                ),
                track(
                    "td-noir-002",
                    "Blue Cathode",
                    "Mercury Scene",
                    "downtempo",
                    true,
                    audioFeatures("sp-track-blue-cathode", "spotify_match", 239000, 9, 1, 4, 0.41, 0.57, 0.48, 0.14, 0.12, -11.2, 0.06, 92.6, 0.37)
                ),
                track(
                    "td-noir-003",
                    "Contour Dust",
                    "Soft Relay",
                    "ambient-pop",
                    false,
                    audioFeatures("sp-track-contour-dust", "fallback_generated", 261000, 4, 1, 4, 0.46, 0.5, 0.43, 0.25, 0.08, -12.3, 0.03, 86.4, 0.29)
                )
            ),
            playlist(
                "tidal-sunrise-district",
                "Sunrise District",
                "tidal",
                "tidal-editorial",
                "Brighter blend for morning ramp-up and GMS discovery seeding.",
                track(
                    "td-sunrise-001",
                    "Morning Switchboard",
                    "Harbor FM",
                    "electropop",
                    true,
                    audioFeatures("sp-track-morning-switchboard", "spotify_match", 211000, 6, 1, 4, 0.18, 0.73, 0.74, 0.01, 0.15, -8.0, 0.04, 117.9, 0.7)
                ),
                track(
                    "td-sunrise-002",
                    "Window Current",
                    "Delta Ribbon",
                    "synth-pop",
                    true,
                    audioFeatures("sp-track-window-current", "spotify_match", 205000, 11, 1, 4, 0.21, 0.71, 0.69, 0.02, 0.13, -8.3, 0.05, 120.4, 0.65)
                ),
                track(
                    "td-sunrise-003",
                    "Glass Ferry",
                    "June Static",
                    "dream-pop",
                    false,
                    audioFeatures("sp-track-glass-ferry", "fallback_generated", 227000, 8, 1, 4, 0.33, 0.6, 0.55, 0.07, 0.11, -9.6, 0.04, 105.7, 0.59)
                )
            )
        ),
        "youtube-music",
        List.of(
            playlist(
                "ytm-loop-late-code",
                "Late Code Loop",
                "youtube-music",
                "youtube-music-library",
                "Saved loop for focused night sessions with melodic electronic edges.",
                track(
                    "ytm-code-001",
                    "Hex Horizon",
                    "Signal Motel",
                    "synthwave",
                    true,
                    audioFeatures("sp-track-hex-horizon", "spotify_match", 224000, 9, 1, 4, 0.17, 0.72, 0.77, 0.01, 0.12, -7.4, 0.04, 118.1, 0.62)
                ),
                track(
                    "ytm-code-002",
                    "Cursor Bloom",
                    "Neon Minutes",
                    "electropop",
                    true,
                    audioFeatures("sp-track-cursor-bloom", "spotify_match", 208000, 4, 1, 4, 0.22, 0.7, 0.73, 0.02, 0.14, -8.2, 0.05, 114.6, 0.58)
                ),
                track(
                    "ytm-code-003",
                    "Window Delay",
                    "Tape District",
                    "indietronica",
                    false,
                    audioFeatures("sp-track-window-delay", "fallback_generated", 237000, 7, 0, 4, 0.28, 0.63, 0.59, 0.08, 0.1, -9.8, 0.04, 103.7, 0.44)
                )
            ),
            playlist(
                "ytm-morning-burst",
                "Morning Burst Mix",
                "youtube-music",
                "youtube-music-mix",
                "Brighter recommendation mix for upbeat PMS seed expansion.",
                track(
                    "ytm-morning-001",
                    "Sunline Arcade",
                    "Velvet Driver",
                    "synth-pop",
                    true,
                    audioFeatures("sp-track-sunline-arcade", "spotify_match", 211000, 1, 1, 4, 0.12, 0.78, 0.81, 0.0, 0.11, -7.1, 0.04, 123.0, 0.74)
                ),
                track(
                    "ytm-morning-002",
                    "Routine Sparks",
                    "Color Index",
                    "dance-pop",
                    true,
                    audioFeatures("sp-track-routine-sparks", "spotify_match", 205000, 6, 1, 4, 0.15, 0.76, 0.79, 0.01, 0.16, -7.5, 0.05, 120.8, 0.7)
                ),
                track(
                    "ytm-morning-003",
                    "Sky Receipt",
                    "June Motor",
                    "dream-pop",
                    false,
                    audioFeatures("sp-track-sky-receipt", "fallback_generated", 229000, 10, 1, 4, 0.31, 0.61, 0.56, 0.04, 0.09, -9.4, 0.03, 106.3, 0.55)
                )
            )
        )
    );

    public List<ImportCandidatePlaylist> getAvailablePlaylists(String platformId) {
        return CATALOG.getOrDefault(platformId, List.of());
    }

    public Optional<ImportCandidatePlaylist> findPlaylist(String platformId, String externalPlaylistId) {
        return getAvailablePlaylists(platformId).stream()
            .filter(playlist -> playlist.externalPlaylistId().equals(externalPlaylistId))
            .findFirst();
    }

    public record ImportCandidatePlaylist(
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        String curator,
        String description,
        String coverImageUrl,
        String platformExternalUrl,
        String platformUri,
        int trackCount,
        List<ImportCandidateTrack> tracks
    ) {

        public ImportCandidatePlaylist {
            tracks = tracks == null ? List.of() : List.copyOf(tracks);
            if (trackCount <= 0 && !tracks.isEmpty()) {
                trackCount = tracks.size();
            }
            if (trackCount < 0) {
                trackCount = 0;
            }
        }

        public ImportCandidatePlaylist withoutTracks() {
            return new ImportCandidatePlaylist(
                externalPlaylistId,
                title,
                sourcePlatform,
                curator,
                description,
                coverImageUrl,
                platformExternalUrl,
                platformUri,
                trackCount,
                List.of()
            );
        }
    }

    public record ImportCandidateTrack(
        String externalTrackId,
        String title,
        String artistName,
        String primaryGenre,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        String platformUri,
        String previewUrl,
        String isrc,
        String spotifyTrackId,
        String spotifyUri,
        String tidalTrackId,
        String tidalUri,
        String preferredPlaybackPlatform,
        String playbackTargetStatus,
        boolean seed,
        PmsTrackAudioFeatures audioFeatures
    ) {
        public ImportCandidateTrack {
            spotifyTrackId = firstNonBlank(spotifyTrackId, nativeTrackId(sourcePlatformFromUri(platformUri), externalTrackId, "spotify"));
            spotifyUri = firstNonBlank(spotifyUri, nativeUri(platformUri, "spotify"));
            tidalTrackId = firstNonBlank(tidalTrackId, nativeTrackId(sourcePlatformFromUri(platformUri), externalTrackId, "tidal"));
            tidalUri = firstNonBlank(tidalUri, nativeUri(platformUri, "tidal"));
            preferredPlaybackPlatform = firstNonBlank(
                preferredPlaybackPlatform,
                firstNonBlank(sourcePlatformFromUri(platformUri), firstNonBlank(hasText(tidalTrackId) ? "tidal" : null, hasText(spotifyTrackId) ? "spotify" : null))
            );
            playbackTargetStatus = firstNonBlank(
                playbackTargetStatus,
                hasText(sourcePlatformFromUri(platformUri)) ? "native" : (hasText(spotifyTrackId) || hasText(tidalTrackId) ? "resolved" : "unresolved")
            );
        }

        public ImportCandidateTrack(
            String externalTrackId,
            String title,
            String artistName,
            String primaryGenre,
            String albumTitle,
            String albumImageUrl,
            String platformExternalUrl,
            String platformUri,
            String previewUrl,
            boolean seed,
            PmsTrackAudioFeatures audioFeatures
        ) {
            this(
                externalTrackId,
                title,
                artistName,
                primaryGenre,
                albumTitle,
                albumImageUrl,
                platformExternalUrl,
                platformUri,
                previewUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                seed,
                audioFeatures
            );
        }
    }

    private static String sourcePlatformFromUri(String platformUri) {
        if (platformUri == null || platformUri.isBlank()) {
            return null;
        }
        if (platformUri.startsWith("spotify:")) {
            return "spotify";
        }
        if (platformUri.startsWith("tidal:")) {
            return "tidal";
        }
        return null;
    }

    private static String nativeTrackId(String sourcePlatform, String externalTrackId, String targetPlatform) {
        return targetPlatform.equals(sourcePlatform) ? externalTrackId : null;
    }

    private static String nativeUri(String platformUri, String targetPlatform) {
        String sourcePlatform = sourcePlatformFromUri(platformUri);
        return targetPlatform.equals(sourcePlatform) ? platformUri : null;
    }

    private static String firstNonBlank(String first, String second) {
        return hasText(first) ? first : (hasText(second) ? second : null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static ImportCandidatePlaylist playlist(
        String externalPlaylistId,
        String title,
        String sourcePlatform,
        String curator,
        String description,
        ImportCandidateTrack... tracks
    ) {
        return new ImportCandidatePlaylist(
            externalPlaylistId,
            title,
            sourcePlatform,
            curator,
            description,
            null,
            null,
            null,
            tracks.length,
            List.of(tracks)
        );
    }

    private static ImportCandidateTrack track(
        String externalTrackId,
        String title,
        String artistName,
        String primaryGenre,
        boolean seed,
        PmsTrackAudioFeatures audioFeatures
    ) {
        return new ImportCandidateTrack(
            externalTrackId,
            title,
            artistName,
            primaryGenre,
            null,
            null,
            null,
            null,
            null,
            seed,
            audioFeatures
        );
    }

    private static PmsTrackAudioFeatures audioFeatures(
        String spotifyTrackId,
        String source,
        int durationMs,
        int musicalKey,
        int mode,
        int timeSignature,
        double acousticness,
        double danceability,
        double energy,
        double instrumentalness,
        double liveness,
        double loudness,
        double speechiness,
        double tempo,
        double valence
    ) {
        return new PmsTrackAudioFeatures(
            spotifyTrackId,
            source,
            true,
            "https://api.spotify.com/v1/audio-analysis/%s".formatted(spotifyTrackId),
            "https://api.spotify.com/v1/tracks/%s".formatted(spotifyTrackId),
            "spotify:track:%s".formatted(spotifyTrackId),
            "audio_features",
            durationMs,
            musicalKey,
            mode,
            timeSignature,
            acousticness,
            danceability,
            energy,
            instrumentalness,
            liveness,
            loudness,
            speechiness,
            tempo,
            valence,
            RESOLVED_AT
        );
    }
}
