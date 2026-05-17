package io.myforevermusic.api.modules.melon.infrastructure.scraping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fetches and parses Melon's public Hot 100 page. The site does not expose a
 * public JSON feed, so we scrape the HTML using Jsoup. Parser logic is kept
 * pure (string -> List<ScrapedTrack>) so tests can exercise it with fixture
 * markup without touching the network.
 */
@Component
public class MelonChartScraper {

    private static final Logger log = LoggerFactory.getLogger(MelonChartScraper.class);
    private static final String CHART_URL = "https://www.melon.com/chart/index.htm";
    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)"
            + " Chrome/126.0.0.0 Safari/537.36";

    public record ScrapedTrack(
        int rank,
        String melonSongId,
        String title,
        String artistName,
        String albumTitle,
        String imageUrl,
        String songExternalUrl
    ) {
    }

    public List<ScrapedTrack> fetch() {
        try {
            Document document = Jsoup.connect(CHART_URL)
                .userAgent(USER_AGENT)
                .timeout(15_000)
                .get();
            return parse(document);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch Melon chart HTML.", exception);
        }
    }

    public List<ScrapedTrack> parse(String html) {
        return parse(Jsoup.parse(html));
    }

    private List<ScrapedTrack> parse(Document document) {
        List<ScrapedTrack> tracks = new ArrayList<>();

        Elements rows = document.select("table tbody tr.lst50, table tbody tr.lst100");
        for (Element row : rows) {
            try {
                ScrapedTrack track = parseRow(row);
                if (track != null) {
                    tracks.add(track);
                }
            } catch (RuntimeException exception) {
                log.warn("Failed to parse a Melon chart row, skipping. data-song-no={}",
                    row.attr("data-song-no"), exception);
            }
        }

        if (tracks.isEmpty()) {
            log.warn("Melon chart parser produced 0 tracks — page layout may have changed.");
        }
        return tracks;
    }

    private ScrapedTrack parseRow(Element row) {
        String rankText = textOf(row.selectFirst("span.rank"));
        if (rankText == null || rankText.isBlank()) {
            return null;
        }
        int rank;
        try {
            rank = Integer.parseInt(rankText.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException nfe) {
            return null;
        }

        String songId = firstNonBlank(
            row.attr("data-song-no"),
            attrOf(row.selectFirst("a[href*=goSongDetail]"), "href")
        );
        if (songId != null) {
            songId = songId.replaceAll(".*goSongDetail\\(['\"]?([0-9]+).*", "$1");
            songId = songId.replaceAll("[^0-9]", "");
            if (songId.isBlank()) {
                songId = null;
            }
        }

        String title = textOf(row.selectFirst("div.ellipsis.rank01 a"));
        if (title == null || title.isBlank()) {
            title = textOf(row.selectFirst("div.ellipsis.rank01 span"));
        }
        if (title == null || title.isBlank()) {
            return null;
        }

        String artist = textOf(row.selectFirst("div.ellipsis.rank02 a"));
        if (artist == null || artist.isBlank()) {
            artist = textOf(row.selectFirst("div.ellipsis.rank02 span"));
        }
        if (artist == null || artist.isBlank()) {
            artist = "Unknown artist";
        }

        String album = textOf(row.selectFirst("div.ellipsis.rank03 a"));
        if (album != null && album.isBlank()) {
            album = null;
        }

        String image = attrOf(row.selectFirst("a.image_typeAll img"), "src");
        if (image == null) {
            image = attrOf(row.selectFirst("td a img"), "src");
        }

        String songExternalUrl = songId == null
            ? null
            : "https://www.melon.com/song/detail.htm?songId=" + songId;

        return new ScrapedTrack(rank, songId, title, artist, album, image, songExternalUrl);
    }

    private static String textOf(Element element) {
        return element == null ? null : element.text().trim();
    }

    private static String attrOf(Element element, String attribute) {
        if (element == null) {
            return null;
        }
        String value = element.attr(attribute);
        return value == null || value.isBlank() ? null : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
