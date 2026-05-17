package io.myforevermusic.api.modules.mainpage.application;

import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionSignalEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionSignalRepository;
import io.myforevermusic.api.modules.mainpage.infrastructure.MagazineArticleEnricher;
import io.myforevermusic.api.modules.mainpage.presentation.MagazineArticleResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MagazineArticleService {

    private static final int SCAN_MULTIPLIER = 4;
    private static final int MAX_SCAN = 200;

    private final EmsAcquisitionSignalRepository signalRepository;
    private final MagazineArticleEnricher enricher;

    public MagazineArticleService(
        EmsAcquisitionSignalRepository signalRepository,
        MagazineArticleEnricher enricher
    ) {
        this.signalRepository = signalRepository;
        this.enricher = enricher;
    }

    @Transactional(readOnly = true)
    public List<MagazineArticleResponse> findRecent(int limit) {
        int safeLimit = Math.max(1, limit);
        int scanLimit = Math.min(MAX_SCAN, Math.max(safeLimit * SCAN_MULTIPLIER, safeLimit));
        List<EmsAcquisitionSignalEntity> rows = signalRepository.findRecentArticles(PageRequest.of(0, scanLimit));

        List<MagazineArticleResponse> articles = new ArrayList<>();
        LinkedHashSet<String> seenUrls = new LinkedHashSet<>();
        for (EmsAcquisitionSignalEntity row : rows) {
            String url = row.getArticleUrl();
            if (url == null || url.isBlank() || !seenUrls.add(url)) {
                continue;
            }
            String title = row.getArticleTitle();
            String rationale = row.getRationale();
            String description = enricher.fetchDescription(url);
            articles.add(new MagazineArticleResponse(
                row.getSourceName(),
                row.getSourceUrl(),
                url,
                title,
                enricher.translateToKorean(title),
                description,
                enricher.translateToKorean(description),
                rationale,
                enricher.translateToKorean(rationale),
                enricher.fetchImageUrl(url),
                row.getCreatedAt()
            ));
            if (articles.size() >= safeLimit) {
                break;
            }
        }
        return articles;
    }
}
