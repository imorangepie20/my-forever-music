package io.myforevermusic.api.modules.pms.application;

import io.myforevermusic.api.modules.pms.presentation.PmsWorkspaceBootstrapResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PmsStaticWorkspaceBootstrapSource implements PmsWorkspaceBootstrapSource {

    @Override
    public Optional<PmsWorkspaceBootstrapResponse> load() {
        return Optional.of(
            new PmsWorkspaceBootstrapResponse(
                "api",
                "ok",
                Instant.now(),
                new PmsWorkspaceBootstrapResponse.WorkspaceDefaults(
                    "user-001",
                    "playlist-001",
                    List.of("track-alpha", "track-beta"),
                    List.of("Artist One", "Artist Two"),
                    List.of("synth-pop", "dream-pop")
                ),
                List.of(
                    new PmsWorkspaceBootstrapResponse.PlaylistOption(
                        "playlist-001",
                        "Forever Midnight Drive",
                        "spotify",
                        42,
                        "system",
                        "High replay consistency and strong synth-pop overlap."
                    ),
                    new PmsWorkspaceBootstrapResponse.PlaylistOption(
                        "playlist-002",
                        "Soft Signal Bloom",
                        "apple-music",
                        28,
                        "editorial",
                        "Good candidate for calm and discovery-focused sessions."
                    ),
                    new PmsWorkspaceBootstrapResponse.PlaylistOption(
                        "playlist-003",
                        "Velvet Motion Archive",
                        "tidal",
                        35,
                        "user",
                        "Dense artist affinity and useful late-night reference tracks."
                    )
                ),
                List.of(
                    new PmsWorkspaceBootstrapResponse.TrackSeedSuggestion(
                        "track-alpha",
                        "Track Alpha",
                        "Artist One",
                        "spotify",
                        "sp-track-alpha",
                        true,
                        "spotify_api"
                    ),
                    new PmsWorkspaceBootstrapResponse.TrackSeedSuggestion(
                        "track-beta",
                        "Track Beta",
                        "Artist Two",
                        "apple-music",
                        "sp-track-beta",
                        true,
                        "spotify_match"
                    ),
                    new PmsWorkspaceBootstrapResponse.TrackSeedSuggestion(
                        "track-gamma",
                        "Track Gamma",
                        "Artist Three",
                        "tidal",
                        "sp-track-gamma",
                        true,
                        "spotify_match"
                    )
                ),
                List.of(
                    new PmsWorkspaceBootstrapResponse.ArtistSeedSuggestion(
                        "Artist One",
                        0.94,
                        "Frequently co-occurs with the current seed tracks."
                    ),
                    new PmsWorkspaceBootstrapResponse.ArtistSeedSuggestion(
                        "Artist Two",
                        0.89,
                        "Strong affinity in the same replay cluster."
                    ),
                    new PmsWorkspaceBootstrapResponse.ArtistSeedSuggestion(
                        "Artist Three",
                        0.81,
                        "Useful expansion candidate for discovery bias."
                    )
                ),
                List.of(
                    new PmsWorkspaceBootstrapResponse.GenreSeedSuggestion(
                        "synth-pop",
                        0.92,
                        "Core genre signal from the selected playlist."
                    ),
                    new PmsWorkspaceBootstrapResponse.GenreSeedSuggestion(
                        "dream-pop",
                        0.84,
                        "Supports softer mood transitions in EMS."
                    ),
                    new PmsWorkspaceBootstrapResponse.GenreSeedSuggestion(
                        "indietronica",
                        0.78,
                        "Good expansion edge for GMS preview diversity."
                    )
                )
            )
        );
    }
}
