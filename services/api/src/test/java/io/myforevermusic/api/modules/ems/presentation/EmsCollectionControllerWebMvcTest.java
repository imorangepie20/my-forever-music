package io.myforevermusic.api.modules.ems.presentation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.myforevermusic.api.modules.ems.application.EmsCollectionService;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService.EmsAudioFeatureCoverage;
import io.myforevermusic.api.modules.ems.application.EmsPublicPlaylistDiscoveryScheduler;
import io.myforevermusic.api.modules.ems.application.EmsPublicPlaylistDiscoveryScheduler.EmsPublicPlaylistDiscoveryFailure;
import io.myforevermusic.api.modules.ems.application.EmsPublicPlaylistDiscoveryScheduler.EmsPublicPlaylistDiscoveryRun;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsTrackAudioFeatures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmsCollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmsCollectionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmsCollectionService emsCollectionService;

    @MockBean
    private EmsPublicPlaylistDiscoveryScheduler emsPublicPlaylistDiscoveryScheduler;

    @Test
    void shouldDisplayCollectedPlaylistTracksFromDatabaseOnly() throws Exception {
        EmsCollectedPlaylistEntity playlist = new EmsCollectedPlaylistEntity(
            "playlist-001",
            "Stored EMS Playlist",
            "tidal",
            "TIDAL curator",
            "Stored playlist description",
            null,
            "https://tidal.com/browse/playlist/playlist-001",
            null,
            1,
            "public_pool",
            "editorial",
            Instant.parse("2026-05-10T00:00:00Z")
        );
        ReflectionTestUtils.setField(playlist, "id", 77L);

        EmsCollectedTrackEntity track = new EmsCollectedTrackEntity(
            "track-001",
            "Stored Track",
            "Stored Artist",
            "tidal",
            "USRC17607839",
            "Stored Album",
            null,
            "https://tidal.com/browse/track/track-001",
            "tidal:track:track-001",
            null,
            211000,
            "public_pool",
            Instant.parse("2026-05-10T00:00:00Z"),
            new EmsTrackAudioFeatures(
                null,
                "unavailable",
                false,
                null,
                null,
                null,
                "audio_features",
                211000,
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
                Instant.parse("2026-05-10T00:00:00Z")
            )
        );
        ReflectionTestUtils.setField(track, "id", 88L);

        when(emsCollectionService.getCollectedPlaylist(77L)).thenReturn(playlist);
        when(emsCollectionService.getTracksForPlaylist(77L)).thenReturn(List.of(track));
        when(emsCollectionService.getAudioFeatureCoverage(77L)).thenReturn(new EmsAudioFeatureCoverage(1, 0, 1, 0.0));

        mockMvc.perform(get("/api/v1/ems/collection/playlists/77").param("user_id", "user-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.playlist.id").value(77))
            .andExpect(jsonPath("$.tracks[0].id").value(88))
            .andExpect(jsonPath("$.tracks[0].isrc").value("USRC17607839"))
            .andExpect(jsonPath("$.tracks[0].platform_uri").value("tidal:track:track-001"));

        verify(emsCollectionService).getTracksForPlaylist(77L);
    }

    @Test
    void shouldRunEmsDiscoveryManually() throws Exception {
        when(emsPublicPlaylistDiscoveryScheduler.runNow("user-001", List.of("tidal"), List.of("jazz"), 2))
            .thenReturn(new EmsPublicPlaylistDiscoveryRun(
                "manual",
                "completed_with_failures",
                Instant.parse("2026-05-10T01:00:00Z"),
                Instant.parse("2026-05-10T01:00:10Z"),
                List.of("tidal"),
                List.of("jazz"),
                2,
                1,
                24,
                List.of(new EmsPublicPlaylistDiscoveryFailure("tidal", "jazz", "token expired")),
                "EMS public playlist discovery completed_with_failures."
            ));

        mockMvc.perform(post("/api/v1/ems/collection/discovery/run")
                .contentType("application/json")
                .content("""
                    {
                      "user_id": "user-001",
                      "platforms": ["tidal"],
                      "seed_queries": ["jazz"],
                      "per_query_limit": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("completed_with_failures"))
            .andExpect(jsonPath("$.trigger").value("manual"))
            .andExpect(jsonPath("$.platforms[0]").value("tidal"))
            .andExpect(jsonPath("$.seed_queries[0]").value("jazz"))
            .andExpect(jsonPath("$.per_query_limit").value(2))
            .andExpect(jsonPath("$.collected_playlist_count").value(1))
            .andExpect(jsonPath("$.collected_track_count").value(24))
            .andExpect(jsonPath("$.failures[0].platform_id").value("tidal"))
            .andExpect(jsonPath("$.failures[0].message").value("token expired"));
    }

    @Test
    void shouldExposeEmsDiscoveryStatusBeforeFirstRun() throws Exception {
        when(emsPublicPlaylistDiscoveryScheduler.lastRun()).thenReturn(null);

        mockMvc.perform(get("/api/v1/ems/collection/discovery/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("not_run"))
            .andExpect(jsonPath("$.message").value("EMS public playlist discovery has not run in this API process."));
    }
}
