package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.application.EmsLooseTrackPlaylistService.LooseTrackPlaylistMaterializationResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EmsLooseTrackPlaylistSchedulerTest {

    @Test
    void shouldSkipUntilEnoughLooseTracksAccumulate() {
        EmsLooseTrackPlaylistService playlistService = Mockito.mock(EmsLooseTrackPlaylistService.class);
        EmsLooseTrackPlaylistProperties properties = properties();
        when(playlistService.countUnassignedTracks()).thenReturn(12L);

        EmsLooseTrackPlaylistScheduler scheduler = new EmsLooseTrackPlaylistScheduler(playlistService, properties);

        EmsLooseTrackPlaylistScheduler.EmsLooseTrackPlaylistRun run = scheduler.runNow();

        assertThat(run.status()).isEqualTo("skipped");
        assertThat(run.message()).contains("threshold is 40");
        assertThat(run.unassignedTrackCountBefore()).isEqualTo(12L);
        assertThat(scheduler.lastRun()).isEqualTo(run);
        verify(playlistService, never()).materializeLooseTracks(Mockito.any(), Mockito.any());
    }

    @Test
    void shouldMaterializeWhenThresholdIsReached() {
        EmsLooseTrackPlaylistService playlistService = Mockito.mock(EmsLooseTrackPlaylistService.class);
        EmsLooseTrackPlaylistProperties properties = properties();
        when(playlistService.countUnassignedTracks()).thenReturn(80L);
        when(playlistService.materializeLooseTracks(5000, 40)).thenReturn(new LooseTrackPlaylistMaterializationResult(
            80L,
            80,
            2,
            80,
            0L,
            Instant.parse("2026-05-18T00:00:00Z"),
            List.of()
        ));

        EmsLooseTrackPlaylistScheduler scheduler = new EmsLooseTrackPlaylistScheduler(playlistService, properties);

        EmsLooseTrackPlaylistScheduler.EmsLooseTrackPlaylistRun run = scheduler.runNow();

        assertThat(run.status()).isEqualTo("completed");
        assertThat(run.createdPlaylistCount()).isEqualTo(2);
        assertThat(run.linkedTrackCount()).isEqualTo(80);
        assertThat(run.unassignedTrackCountAfter()).isZero();
        assertThat(scheduler.lastRun()).isEqualTo(run);
        verify(playlistService).materializeLooseTracks(5000, 40);
    }

    private EmsLooseTrackPlaylistProperties properties() {
        EmsLooseTrackPlaylistProperties properties = new EmsLooseTrackPlaylistProperties();
        properties.setTrackLimit(5000);
        properties.setTracksPerPlaylist(40);
        properties.setMinTrackCount(40);
        return properties;
    }
}
