package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.myforevermusic.api.modules.platform.infrastructure.lastfm.LastFmWebApiClient;
import io.myforevermusic.api.modules.platform.presentation.LastFmSignalPreviewResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LastFmSignalPreviewServiceTest {

    @Test
    void shouldBuildLastFmSignalPreview() {
        LastFmSignalPreviewService service = new LastFmSignalPreviewService(new FakeLastFmWebApiClient());

        var response = service.getPreview("mibeen", "1month", 5, 4);

        assertThat(response.request().username()).isEqualTo("mibeen");
        assertThat(response.summary().source()).isEqualTo("lastfm-public-api");
        assertThat(response.summary().recentTrackCount()).isEqualTo(2);
        assertThat(response.summary().topArtistCount()).isEqualTo(2);
        assertThat(response.summary().topTrackCount()).isEqualTo(2);
        assertThat(response.summary().nowPlaying()).isTrue();
        assertThat(response.insights()).hasSize(3);
        assertThat(response.topArtists()).extracting(LastFmSignalPreviewResponse.TopArtist::artistName)
            .containsExactly("The Midnight", "M83");
    }

    @Test
    void shouldRejectUnsupportedLastFmPeriod() {
        LastFmSignalPreviewService service = new LastFmSignalPreviewService(new FakeLastFmWebApiClient());

        assertThatThrownBy(() -> service.getPreview("mibeen", "2year", 5, 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Last.fm period must be one of");
    }

    private static final class FakeLastFmWebApiClient extends LastFmWebApiClient {

        private FakeLastFmWebApiClient() {
            super(new LastFmProperties(), new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Override
        public LastFmUserProfile getUserProfile(String username) {
            return new LastFmUserProfile(
                username,
                "Woo Sung Jo",
                "KR",
                54189L,
                "https://www.last.fm/user/%s".formatted(username),
                "https://lastfm.example/avatar.jpg",
                Instant.parse("2020-01-01T00:00:00Z")
            );
        }

        @Override
        public List<LastFmRecentTrack> getRecentTracks(String username, int limit) {
            return List.of(
                new LastFmRecentTrack(
                    "Days of Thunder",
                    "The Midnight",
                    "Days of Thunder",
                    "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
                    "https://lastfm.example/days-of-thunder.jpg",
                    true,
                    null,
                    true
                ),
                new LastFmRecentTrack(
                    "Midnight City",
                    "M83",
                    "Hurry Up, We're Dreaming",
                    "https://www.last.fm/music/M83/_/Midnight+City",
                    "https://lastfm.example/midnight-city.jpg",
                    false,
                    Instant.parse("2026-05-04T02:00:00Z"),
                    false
                )
            );
        }

        @Override
        public List<LastFmTopArtist> getTopArtists(String username, String period, int limit) {
            return List.of(
                new LastFmTopArtist("The Midnight", 1, 88L, "https://www.last.fm/music/The+Midnight", null),
                new LastFmTopArtist("M83", 2, 56L, "https://www.last.fm/music/M83", null)
            );
        }

        @Override
        public List<LastFmTopTrack> getTopTracks(String username, String period, int limit) {
            return List.of(
                new LastFmTopTrack(
                    "Days of Thunder",
                    "The Midnight",
                    1,
                    24L,
                    "https://www.last.fm/music/The+Midnight/_/Days+of+Thunder",
                    "https://www.last.fm/music/The+Midnight",
                    null
                ),
                new LastFmTopTrack(
                    "Midnight City",
                    "M83",
                    2,
                    17L,
                    "https://www.last.fm/music/M83/_/Midnight+City",
                    "https://www.last.fm/music/M83",
                    null
                )
            );
        }
    }
}
