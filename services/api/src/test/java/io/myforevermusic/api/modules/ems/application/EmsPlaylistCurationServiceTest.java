package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedPlaylistTrackRepository.PlaylistAudioStatsProjection;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.ArtistAffinity;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.PlatformAffinity;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.Profile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmsPlaylistCurationServiceTest {

    @Mock
    private EmsCollectedPlaylistRepository playlistRepository;

    @Mock
    private EmsCollectedPlaylistTrackRepository playlistTrackRepository;

    @Mock
    private UserPersonalizationProfileStore personalizationProfileStore;

    @Test
    void shouldBuildPersonalizedGenreAndMoodSectionsFromCollectedPool() {
        EmsCollectedPlaylistEntity newJeans = playlist(
            1L,
            "NewJeans Night Drive",
            "tidal",
            "K-Pop chill night city pop",
            "newjeans"
        );
        EmsCollectedPlaylistEntity workout = playlist(
            2L,
            "Workout Dance Hits",
            "spotify",
            "EDM party club boost",
            "workout"
        );
        EmsCollectedPlaylistEntity indie = playlist(
            3L,
            "Indie Guitar Morning",
            "tidal",
            "indie alternative guitar",
            "indie"
        );
        when(playlistRepository.findRecentWithTracksBySourcePlatforms(eq(List.of("tidal", "spotify")), any(Pageable.class)))
            .thenReturn(List.of(newJeans, workout, indie));
        when(playlistTrackRepository.findAudioStatsByPlaylistIds(List.of(1L, 2L, 3L)))
            .thenReturn(List.of(
                stats(1L, 24, 18L, 0.42, 0.6, 0.71, 0.45, 0.08),
                stats(2L, 32, 25L, 0.82, 0.7, 0.78, 0.12, 0.1),
                stats(3L, 18, 9L, 0.55, 0.52, 0.5, 0.35, 0.06)
            ));
        when(personalizationProfileStore.findByUserId("user-001"))
            .thenReturn(Optional.of(new Profile(
                10L,
                "user-001",
                List.of(new ArtistAffinity("NewJeans", 5.0, 3)),
                List.of(new PlatformAffinity("tidal", 4.0, 4)),
                7,
                Instant.parse("2026-05-15T00:00:00Z"),
                Instant.parse("2026-05-15T01:00:00Z")
            )));

        EmsPlaylistCurationService.EmsPlaylistCurationResult result = service()
            .getPlaylistSections("user-001", List.of("tidal", "spotify"), 3);

        assertThat(result.personalized()).isTrue();
        assertThat(result.titleModel()).isEqualTo(EmsPlaylistCurationService.TITLE_MODEL);
        assertThat(result.sections()).extracting(EmsPlaylistCurationService.EmsPlaylistSection::sectionId)
            .contains("personalized-signal", "mood-high-energy", "mood-late-night", "genre-k-pop");
        assertThat(result.sections().getFirst().title()).contains("NewJeans");
        assertThat(section(result, "personalized-signal").playlists().getFirst().playlist().getTitle())
            .isEqualTo("NewJeans Night Drive");
        assertThat(section(result, "mood-high-energy").playlists()).extracting(item -> item.playlist().getTitle())
            .contains("Workout Dance Hits");
        assertThat(section(result, "mood-late-night").playlists()).extracting(item -> item.playlist().getTitle())
            .contains("NewJeans Night Drive");
    }

    @Test
    void shouldReturnGeneralSectionsWhenUserHasNoProfile() {
        EmsCollectedPlaylistEntity playlist = playlist(
            4L,
            "Jazz Focus Room",
            "tidal",
            "jazz focus acoustic",
            "focus"
        );
        when(playlistRepository.findRecentWithTracks(any(Pageable.class)))
            .thenReturn(List.of(playlist));
        when(playlistTrackRepository.findAudioStatsByPlaylistIds(List.of(4L)))
            .thenReturn(List.of(stats(4L, 16, 10L, 0.38, 0.48, 0.35, 0.52, 0.04)));

        EmsPlaylistCurationService.EmsPlaylistCurationResult result = service()
            .getPlaylistSections(null, List.of(), 3);

        assertThat(result.personalized()).isFalse();
        assertThat(result.sections()).extracting(EmsPlaylistCurationService.EmsPlaylistSection::categoryType)
            .contains("mood", "genre", "quality", "fresh")
            .doesNotContain("personalized");
    }

    private EmsPlaylistCurationService service() {
        return new EmsPlaylistCurationService(
            playlistRepository,
            playlistTrackRepository,
            personalizationProfileStore
        );
    }

    private EmsPlaylistCurationService.EmsPlaylistSection section(
        EmsPlaylistCurationService.EmsPlaylistCurationResult result,
        String sectionId
    ) {
        return result.sections().stream()
            .filter(section -> section.sectionId().equals(sectionId))
            .findFirst()
            .orElseThrow();
    }

    private EmsCollectedPlaylistEntity playlist(
        Long id,
        String title,
        String platformId,
        String description,
        String searchQuery
    ) {
        EmsCollectedPlaylistEntity playlist = new EmsCollectedPlaylistEntity(
            "playlist-" + id,
            title,
            platformId,
            platformId + " editors",
            description,
            null,
            "https://example.com/playlists/" + id,
            "spotify".equals(platformId) ? "spotify:playlist:playlist-" + id : null,
            24,
            "acquisition_pool",
            searchQuery,
            Instant.parse("2026-05-10T00:00:00Z")
        );
        ReflectionTestUtils.setField(playlist, "id", id);
        return playlist;
    }

    private PlaylistAudioStatsProjection stats(
        Long playlistId,
        long trackCount,
        Long filledTrackCount,
        Double averageEnergy,
        Double averageValence,
        Double averageDanceability,
        Double averageAcousticness,
        Double averageSpeechiness
    ) {
        return new PlaylistAudioStatsProjection() {
            @Override
            public Long getPlaylistId() {
                return playlistId;
            }

            @Override
            public long getTrackCount() {
                return trackCount;
            }

            @Override
            public Long getFilledTrackCount() {
                return filledTrackCount;
            }

            @Override
            public Double getAverageEnergy() {
                return averageEnergy;
            }

            @Override
            public Double getAverageValence() {
                return averageValence;
            }

            @Override
            public Double getAverageDanceability() {
                return averageDanceability;
            }

            @Override
            public Double getAverageAcousticness() {
                return averageAcousticness;
            }

            @Override
            public Double getAverageSpeechiness() {
                return averageSpeechiness;
            }
        };
    }
}
