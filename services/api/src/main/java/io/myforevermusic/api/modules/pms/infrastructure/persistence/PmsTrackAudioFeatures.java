package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class PmsTrackAudioFeatures {

    @Column(name = "audio_feature_track_id", length = 100)
    private String audioFeatureTrackId;

    @Column(name = "audio_feature_source", nullable = false, length = 50)
    private String audioFeatureSource;

    @Column(name = "audio_features_filled", nullable = false)
    private boolean audioFeaturesFilled;

    @Column(name = "audio_analysis_url", length = 300)
    private String analysisUrl;

    @Column(name = "audio_track_href", length = 300)
    private String trackHref;

    @Column(name = "audio_track_uri", length = 200)
    private String audioTrackUri;

    @Column(name = "audio_feature_type", nullable = false, length = 30)
    private String featureType;

    @Column(name = "audio_duration_ms")
    private Integer durationMs;

    @Column(name = "audio_key")
    private Integer musicalKey;

    @Column(name = "audio_mode")
    private Integer mode;

    @Column(name = "audio_time_signature")
    private Integer timeSignature;

    @Column(name = "audio_acousticness")
    private Double acousticness;

    @Column(name = "audio_danceability")
    private Double danceability;

    @Column(name = "audio_energy")
    private Double energy;

    @Column(name = "audio_instrumentalness")
    private Double instrumentalness;

    @Column(name = "audio_liveness")
    private Double liveness;

    @Column(name = "audio_loudness")
    private Double loudness;

    @Column(name = "audio_speechiness")
    private Double speechiness;

    @Column(name = "audio_tempo")
    private Double tempo;

    @Column(name = "audio_valence")
    private Double valence;

    @Column(name = "audio_resolved_at")
    private Instant resolvedAt;

    protected PmsTrackAudioFeatures() {
    }

    public PmsTrackAudioFeatures(
        String audioFeatureTrackId,
        String audioFeatureSource,
        boolean audioFeaturesFilled,
        String analysisUrl,
        String trackHref,
        String audioTrackUri,
        String featureType,
        Integer durationMs,
        Integer musicalKey,
        Integer mode,
        Integer timeSignature,
        Double acousticness,
        Double danceability,
        Double energy,
        Double instrumentalness,
        Double liveness,
        Double loudness,
        Double speechiness,
        Double tempo,
        Double valence,
        Instant resolvedAt
    ) {
        this.audioFeatureTrackId = audioFeatureTrackId;
        this.audioFeatureSource = audioFeatureSource == null ? "unresolved" : audioFeatureSource;
        this.audioFeaturesFilled = audioFeaturesFilled;
        this.analysisUrl = analysisUrl;
        this.trackHref = trackHref;
        this.audioTrackUri = audioTrackUri;
        this.featureType = featureType == null ? "audio_features" : featureType;
        this.durationMs = durationMs;
        this.musicalKey = musicalKey;
        this.mode = mode;
        this.timeSignature = timeSignature;
        this.acousticness = acousticness;
        this.danceability = danceability;
        this.energy = energy;
        this.instrumentalness = instrumentalness;
        this.liveness = liveness;
        this.loudness = loudness;
        this.speechiness = speechiness;
        this.tempo = tempo;
        this.valence = valence;
        this.resolvedAt = resolvedAt;
    }

    public static PmsTrackAudioFeatures unresolved() {
        return new PmsTrackAudioFeatures(
            null,
            "unresolved",
            false,
            null,
            null,
            null,
            "audio_features",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public String getAudioFeatureTrackId() {
        return audioFeatureTrackId;
    }

    public String getSpotifyTrackId() {
        return audioFeatureTrackId;
    }

    public String getAudioFeatureSource() {
        return audioFeatureSource;
    }

    public boolean isAudioFeaturesFilled() {
        return audioFeaturesFilled;
    }

    public String getAnalysisUrl() {
        return analysisUrl;
    }

    public String getTrackHref() {
        return trackHref;
    }

    public String getSpotifyUri() {
        return audioTrackUri;
    }

    public String getAudioTrackUri() {
        return audioTrackUri;
    }

    public String getFeatureType() {
        return featureType;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public Integer getMusicalKey() {
        return musicalKey;
    }

    public Integer getMode() {
        return mode;
    }

    public Integer getTimeSignature() {
        return timeSignature;
    }

    public Double getAcousticness() {
        return acousticness;
    }

    public Double getDanceability() {
        return danceability;
    }

    public Double getEnergy() {
        return energy;
    }

    public Double getInstrumentalness() {
        return instrumentalness;
    }

    public Double getLiveness() {
        return liveness;
    }

    public Double getLoudness() {
        return loudness;
    }

    public Double getSpeechiness() {
        return speechiness;
    }

    public Double getTempo() {
        return tempo;
    }

    public Double getValence() {
        return valence;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public boolean isComplete() {
        return audioFeaturesFilled
            && durationMs != null
            && musicalKey != null
            && mode != null
            && acousticness != null
            && danceability != null
            && energy != null
            && instrumentalness != null
            && liveness != null
            && loudness != null
            && speechiness != null
            && tempo != null
            && valence != null
            && resolvedAt != null;
    }
}
