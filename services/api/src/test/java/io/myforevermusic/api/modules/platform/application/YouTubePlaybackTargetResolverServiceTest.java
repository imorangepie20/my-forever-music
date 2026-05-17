package io.myforevermusic.api.modules.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.common.error.ApiResourceNotFoundException;
import io.myforevermusic.api.modules.platform.infrastructure.youtube.YouTubeDataApiClient;
import io.myforevermusic.api.modules.platform.infrastructure.youtube.YouTubeDataApiClient.YouTubeVideoCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class YouTubePlaybackTargetResolverServiceTest {

    @Test
    void shouldPreferEmbeddableOfficialMusicVideoOverKaraokeCandidate() {
        YouTubeDataApiClient client = mock(YouTubeDataApiClient.class);
        YouTubePlaybackTargetResolverService service = new YouTubePlaybackTargetResolverService(client);
        when(client.searchEmbeddableVideos("라이딩 하성운", 12))
            .thenReturn(List.of(
                candidate("_D_Pr2kmYLo", "[TJ노래방] 라이딩(Riding) - 하성운(Feat.개코)", "TJ노래방 공식 유튜브채널", 237_000),
                candidate("rNeZcq59-HM", "Riding (feat. Gaeko)", "하성운 HA SUNG WOON Official", 240_000)
            ));

        var target = service.resolve(new YouTubePlaybackTargetResolverService.TrackQuery(
            "라이딩",
            "하성운",
            "flo",
            "424991128",
            null,
            null,
            null,
            null,
            240_000,
            List.of()
        ));

        assertThat(target.youtubeVideoId()).isEqualTo("rNeZcq59-HM");
        assertThat(target.youtubeUrl()).isEqualTo("https://www.youtube.com/watch?v=rNeZcq59-HM");
        verify(client).searchEmbeddableVideos("라이딩 하성운", 12);
    }

    @Test
    void shouldFailWhenOnlyExcludedVideoMatches() {
        YouTubeDataApiClient client = mock(YouTubeDataApiClient.class);
        YouTubePlaybackTargetResolverService service = new YouTubePlaybackTargetResolverService(client);
        when(client.searchEmbeddableVideos("Midnight Signal Neon Bloom", 12))
            .thenReturn(List.of(candidate("abcDEF12345", "Midnight Signal Official Audio", "Neon Bloom Official", 218_000)));

        assertThatThrownBy(() -> service.resolve(new YouTubePlaybackTargetResolverService.TrackQuery(
            "Midnight Signal",
            "Neon Bloom",
            "spotify",
            "sp-track-001",
            null,
            "spotify-track",
            null,
            null,
            218_000,
            List.of("abcDEF12345")
        ))).isInstanceOf(ApiResourceNotFoundException.class)
            .hasMessageContaining("No embeddable YouTube video");
    }

    private YouTubeVideoCandidate candidate(String videoId, String title, String channelTitle, int durationMs) {
        return new YouTubeVideoCandidate(
            videoId,
            title,
            channelTitle,
            "%s by %s".formatted(title, channelTitle),
            "https://i.ytimg.com/vi/%s/hqdefault.jpg".formatted(videoId),
            durationMs
        );
    }
}
