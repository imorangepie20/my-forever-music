package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class PmsTrackSpotifyAudioFeatures {

    @Column(name = "spotify_track_id", length = 100)
    private String spotifyTrackId;

    @Column(name = "spotify_audio_feature_source", nullable = false, length = 50)
    private String audioFeatureSource;

    @Column(name = "spotify_audio_features_filled", nullable = false)
    private boolean audioFeaturesFilled;

    @Column(name = "spotify_analysis_url", length = 300)
    private String analysisUrl;

    @Column(name = "spotify_track_href", length = 300)
    private String trackHref;

    @Column(name = "spotify_uri", length = 200)
    private String spotifyUri;

    @Column(name = "spotify_feature_type", nullable = false, length = 30)
    private String featureType;

    @Column(name = "spotify_duration_ms")
    private Integer durationMs;

    @Column(name = "spotify_key")
    private Integer musicalKey;

    @Column(name = "spotify_mode")
    private Integer mode;

    @Column(name = "spotify_time_signature")
    private Integer timeSignature;

    @Column(name = "spotify_acousticness")
    private Double acousticness;

    @Column(name = "spotify_danceability")
    private Double danceability;

    @Column(name = "spotify_energy")
    private Double energy;

    @Column(name = "spotify_instrumentalness")
    private Double instrumentalness;

    @Column(name = "spotify_liveness")
    private Double liveness;

    @Column(name = "spotify_loudness")
    private Double loudness;

    @Column(name = "spotify_speechiness")
    private Double speechiness;

    @Column(name = "spotify_tempo")
    private Double tempo;

    @Column(name = "spotify_valence")
    private Double valence;

    @Column(name = "spotify_resolved_at")
    private Instant resolvedAt;

    protected PmsTrackSpotifyAudioFeatures() {
    }

    public PmsTrackSpotifyAudioFeatures(
        String spotifyTrackId,
        String audioFeatureSource,
        boolean audioFeaturesFilled,
        String analysisUrl,
        String trackHref,
        String spotifyUri,
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
        this.spotifyTrackId = spotifyTrackId;
        this.audioFeatureSource = audioFeatureSource == null ? "unresolved" : audioFeatureSource;
        this.audioFeaturesFilled = audioFeaturesFilled;
        this.analysisUrl = analysisUrl;
        this.trackHref = trackHref;
        this.spotifyUri = spotifyUri;
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

    public static PmsTrackSpotifyAudioFeatures unresolved() {
        return new PmsTrackSpotifyAudioFeatures(
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

    public String getSpotifyTrackId() {
        return spotifyTrackId;
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
        return spotifyUri;
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
            && timeSignature != null
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
