package io.myforevermusic.api.modules.melon.infrastructure.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.melon.infrastructure.scraping.MelonChartScraper.ScrapedTrack;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MelonChartScraperTest {

    private final MelonChartScraper scraper = new MelonChartScraper();

    @Test
    void parsesRankRowsFromFixture() throws IOException {
        String html = new String(
            new ClassPathResource("melon/chart-fixture.html").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8
        );

        List<ScrapedTrack> tracks = scraper.parse(html);

        assertThat(tracks).hasSize(3);
        ScrapedTrack first = tracks.get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.title()).isEqualTo("Fixture Song A");
        assertThat(first.artistName()).isEqualTo("Fixture Artist A");
        assertThat(first.albumTitle()).isEqualTo("Fixture Album A");
        assertThat(first.melonSongId()).isEqualTo("12345678");
        assertThat(first.imageUrl()).isEqualTo("https://cdn.melon.com/cover/1.jpg");
        assertThat(first.songExternalUrl()).isEqualTo("https://www.melon.com/song/detail.htm?songId=12345678");

        ScrapedTrack third = tracks.get(2);
        assertThat(third.rank()).isEqualTo(51);
        assertThat(third.title()).isEqualTo("Half Way There");
        assertThat(third.albumTitle()).isNull();
    }

    @Test
    void returnsEmptyListWhenNoRecognisedRows() {
        List<ScrapedTrack> tracks = scraper.parse("<html><body><p>nothing here</p></body></html>");
        assertThat(tracks).isEmpty();
    }
}
