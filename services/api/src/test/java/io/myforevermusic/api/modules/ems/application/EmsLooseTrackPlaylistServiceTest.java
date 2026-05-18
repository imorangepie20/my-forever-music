package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class EmsLooseTrackPlaylistServiceTest {

    @Test
    void shouldMaterializeUnassignedTracksIntoSyntheticPlaylists() {
        EmsCollectedPlaylistRepository playlistRepository = org.mockito.Mockito.mock(EmsCollectedPlaylistRepository.class);
        EmsCollectedTrackRepository trackRepository = org.mockito.Mockito.mock(EmsCollectedTrackRepository.class);
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        EmsCollectedTrackEntity first = track(10L, "spotify", "acquisition_pool", "First Track");
        EmsCollectedTrackEntity second = track(20L, "spotify", "acquisition_pool", "Second Track");
        EmsCollectedPlaylistEntity savedPlaylist = playlist("synthetic:loose:spotify:acquisition_pool:10");
        ReflectionTestUtils.setField(savedPlaylist, "id", 901L);

        when(trackRepository.countUnassignedTracks()).thenReturn(2L, 0L);
        when(trackRepository.findUnassignedTracks(PageRequest.of(0, 500))).thenReturn(List.of(first, second));
        when(playlistRepository.findBySourcePlatformAndExternalPlaylistId(
            "spotify",
            "synthetic:loose:spotify:acquisition_pool:10"
        )).thenReturn(Optional.empty());
        when(playlistRepository.save(any(EmsCollectedPlaylistEntity.class))).thenReturn(savedPlaylist);

        EmsLooseTrackPlaylistService service = new EmsLooseTrackPlaylistService(
            playlistRepository,
            trackRepository,
            jdbcTemplate
        );

        EmsLooseTrackPlaylistService.LooseTrackPlaylistMaterializationResult result =
            service.materializeLooseTracks(null, null);

        assertThat(result.unassignedTrackCountBefore()).isEqualTo(2);
        assertThat(result.selectedTrackCount()).isEqualTo(2);
        assertThat(result.createdPlaylistCount()).isEqualTo(1);
        assertThat(result.linkedTrackCount()).isEqualTo(2);
        assertThat(result.unassignedTrackCountAfter()).isZero();
        assertThat(result.playlists()).singleElement()
            .satisfies(playlist -> {
                assertThat(playlist.playlistId()).isEqualTo(901L);
                assertThat(playlist.sourcePlatform()).isEqualTo("spotify");
                assertThat(playlist.sourceCollection()).isEqualTo("acquisition_pool");
                assertThat(playlist.trackCount()).isEqualTo(2);
            });

        ArgumentCaptor<EmsCollectedPlaylistEntity> playlistCaptor =
            ArgumentCaptor.forClass(EmsCollectedPlaylistEntity.class);
        verify(playlistRepository).save(playlistCaptor.capture());
        assertThat(playlistCaptor.getValue().getCollectionSource())
            .isEqualTo(EmsLooseTrackPlaylistService.COLLECTION_SOURCE);
        ArgumentCaptor<BatchPreparedStatementSetter> batchCaptor =
            ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());
        assertThat(batchCaptor.getValue().getBatchSize()).isEqualTo(2);
    }

    private static EmsCollectedTrackEntity track(Long id, String platform, String source, String title) {
        EmsCollectedTrackEntity track = new EmsCollectedTrackEntity(
            "track-%d".formatted(id),
            title,
            "Artist",
            platform,
            null,
            "Album",
            null,
            null,
            null,
            null,
            180_000,
            source,
            Instant.parse("2026-05-10T00:00:00Z"),
            null
        );
        ReflectionTestUtils.setField(track, "id", id);
        return track;
    }

    private static EmsCollectedPlaylistEntity playlist(String externalPlaylistId) {
        return new EmsCollectedPlaylistEntity(
            externalPlaylistId,
            "EMS 추천 후보 · Spotify · acquisition pool",
            "spotify",
            "EMS Track Materializer",
            "Playlist materialized from EMS tracks that were collected without a source playlist.",
            null,
            null,
            null,
            2,
            EmsLooseTrackPlaylistService.COLLECTION_SOURCE,
            "acquisition_pool",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
