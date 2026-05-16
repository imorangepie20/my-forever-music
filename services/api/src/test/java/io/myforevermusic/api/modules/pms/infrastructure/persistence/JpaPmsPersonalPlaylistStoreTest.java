package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class JpaPmsPersonalPlaylistStoreTest {

    @Test
    void shouldCreateBackingPmsTrackWhenAddingGmsImportedTrackToPersonalPlaylist() {
        PmsPersonalPlaylistRepository playlistRepository = mock(PmsPersonalPlaylistRepository.class);
        PmsPersonalPlaylistTrackRepository playlistTrackRepository = mock(PmsPersonalPlaylistTrackRepository.class);
        PmsUserTrackRepository userTrackRepository = mock(PmsUserTrackRepository.class);
        JpaPmsPersonalPlaylistStore store = new JpaPmsPersonalPlaylistStore(
            playlistRepository,
            playlistTrackRepository,
            userTrackRepository
        );
        PmsPersonalPlaylistEntity playlist = new PmsPersonalPlaylistEntity(
            new PmsPersonalPlaylistStore.CreatePlaylistDraft(
                "user-001",
                "gms-ems-1",
                "EMS Playlist (GMS)",
                "Imported from EMS via GMS"
            )
        );
        ReflectionTestUtils.setField(playlist, "personalPlaylistId", 1L);

        when(playlistRepository.findByUserIdAndPlaylistId("user-001", "gms-ems-1"))
            .thenReturn(Optional.of(playlist));
        when(userTrackRepository.findById("ems-10")).thenReturn(Optional.empty());
        when(userTrackRepository.save(any(PmsUserTrackEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playlistTrackRepository.findByPlaylist_PersonalPlaylistIdAndTrack_TrackId(1L, "ems-10"))
            .thenReturn(Optional.empty());
        when(playlistTrackRepository.countByPlaylist_PersonalPlaylistId(1L)).thenReturn(0L);
        when(playlistRepository.save(playlist)).thenReturn(playlist);
        when(playlistTrackRepository.findByPlaylist_PersonalPlaylistIdOrderBySortOrderAscPersonalPlaylistTrackIdAsc(1L))
            .thenReturn(List.of());

        store.addTrack(new PmsPersonalPlaylistStore.AddTrackDraft(
            "user-001",
            "gms-ems-1",
            new PmsPersonalPlaylistStore.PersonalTrackState(
                "ems-10",
                "spotify-track-010",
                "Keep Me",
                "Artist",
                "spotify",
                "Album",
                null,
                "https://open.spotify.com/track/spotify-track-010",
                "spotify:track:spotify-track-010",
                null,
                "USRC10000010",
                "spotify-track-010",
                "spotify:track:spotify-track-010",
                null,
                null,
                "spotify",
                "native",
                180_000,
                0,
                "gms-playlist-import",
                Instant.parse("2026-05-15T00:00:00Z")
            ),
            "gms-playlist-import"
        ));

        ArgumentCaptor<PmsUserTrackEntity> userTrackCaptor = ArgumentCaptor.forClass(PmsUserTrackEntity.class);
        verify(userTrackRepository).save(userTrackCaptor.capture());
        assertThat(userTrackCaptor.getValue().getTrackId()).isEqualTo("ems-10");
        assertThat(userTrackCaptor.getValue().getExternalTrackId()).isEqualTo("spotify-track-010");

        ArgumentCaptor<PmsPersonalPlaylistTrackEntity> playlistTrackCaptor =
            ArgumentCaptor.forClass(PmsPersonalPlaylistTrackEntity.class);
        verify(playlistTrackRepository).save(playlistTrackCaptor.capture());
        assertThat(playlistTrackCaptor.getValue().getTrack().getTrackId()).isEqualTo("ems-10");
    }
}
