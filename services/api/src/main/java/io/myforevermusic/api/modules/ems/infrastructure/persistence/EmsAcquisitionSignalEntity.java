package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ems_acquisition_signal")
public class EmsAcquisitionSignalEntity {

    public static final String STATUS_READY = "ready";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ems_acquisition_signal_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ems_acquisition_run_id", nullable = false)
    private EmsAcquisitionRunEntity run;

    @Column(name = "source_name", nullable = false, length = 160)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "article_url", length = 500)
    private String articleUrl;

    @Column(name = "article_title", length = 300)
    private String articleTitle;

    @Column(name = "signal_type", nullable = false, length = 50)
    private String signalType;

    @Column(name = "query", nullable = false, length = 200)
    private String query;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "rationale", length = 500)
    private String rationale;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmsAcquisitionSignalEntity() {}

    public EmsAcquisitionSignalEntity(
        EmsAcquisitionRunEntity run,
        String sourceName,
        String sourceUrl,
        String articleUrl,
        String articleTitle,
        String signalType,
        String query,
        BigDecimal confidenceScore,
        String rationale,
        Instant createdAt
    ) {
        this.run = run;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.articleUrl = articleUrl;
        this.articleTitle = articleTitle;
        this.signalType = signalType;
        this.query = query;
        this.confidenceScore = confidenceScore;
        this.rationale = rationale;
        this.status = STATUS_READY;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public EmsAcquisitionRunEntity getRun() { return run; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public String getArticleUrl() { return articleUrl; }
    public String getArticleTitle() { return articleTitle; }
    public String getSignalType() { return signalType; }
    public String getQuery() { return query; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public String getRationale() { return rationale; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
