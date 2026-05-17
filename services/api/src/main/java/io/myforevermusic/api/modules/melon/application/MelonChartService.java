package io.myforevermusic.api.modules.melon.application;

import io.myforevermusic.api.modules.melon.infrastructure.persistence.MelonChartTrackEntity;
import io.myforevermusic.api.modules.melon.infrastructure.persistence.MelonChartTrackRepository;
import io.myforevermusic.api.modules.melon.infrastructure.scraping.MelonChartScraper;
import io.myforevermusic.api.modules.melon.infrastructure.scraping.MelonChartScraper.ScrapedTrack;
import io.myforevermusic.api.modules.melon.presentation.MelonChartTrackResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MelonChartService {

    private static final Logger log = LoggerFactory.getLogger(MelonChartService.class);
    private static final int MAX_LIMIT = 100;

    private final MelonChartTrackRepository repository;
    private final MelonChartScraper scraper;
    private final Clock clock;

    @Autowired
    public MelonChartService(MelonChartTrackRepository repository, MelonChartScraper scraper) {
        this(repository, scraper, Clock.systemUTC());
    }

    MelonChartService(MelonChartTrackRepository repository, MelonChartScraper scraper, Clock clock) {
        this.repository = repository;
        this.scraper = scraper;
        this.clock = clock;
    }

    public List<MelonChartTrackResponse> getTopTracks(int limit) {
        int effective = Math.min(MAX_LIMIT, Math.max(1, limit));
        return repository.findAllByOrderByRankAsc(PageRequest.of(0, effective)).stream()
            .map(MelonChartService::toResponse)
            .toList();
    }

    public List<MelonChartTrackResponse> getFullChart() {
        return repository.findAllByOrderByRankAsc().stream()
            .map(MelonChartService::toResponse)
            .toList();
    }

    @Transactional
    public int refresh() {
        List<ScrapedTrack> scraped = scraper.fetch();
        if (scraped.isEmpty()) {
            log.warn("Melon scrape returned 0 tracks; skipping replace.");
            return 0;
        }
        Instant now = clock.instant();
        repository.deleteAllInBatch();
        List<MelonChartTrackEntity> entities = new ArrayList<>(scraped.size());
        for (ScrapedTrack t : scraped) {
            entities.add(new MelonChartTrackEntity(
                t.rank(),
                t.melonSongId(),
                t.title(),
                t.artistName(),
                t.albumTitle(),
                t.imageUrl(),
                t.songExternalUrl(),
                now
            ));
        }
        repository.saveAll(entities);
        log.info("Melon chart refreshed: {} tracks at {}", entities.size(), now);
        return entities.size();
    }

    private static MelonChartTrackResponse toResponse(MelonChartTrackEntity entity) {
        return new MelonChartTrackResponse(
            entity.getRank(),
            entity.getMelonSongId(),
            entity.getTitle(),
            entity.getArtistName(),
            entity.getAlbumTitle(),
            entity.getImageUrl(),
            entity.getSongExternalUrl(),
            entity.getSnapshotAt()
        );
    }
}
