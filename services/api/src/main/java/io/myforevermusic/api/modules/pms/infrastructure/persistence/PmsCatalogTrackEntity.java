package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pms_track")
public class PmsCatalogTrackEntity {

    @Id
    @Column(name = "track_id", nullable = false, length = 100)
    private String id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 200)
    private String artistName;

    @Column(name = "source_platform", nullable = false, length = 50)
    private String sourcePlatform;

    @Column(name = "primary_genre", length = 100)
    private String primaryGenre;

    @Embedded
    private PmsTrackAudioFeatures audioFeatures;

    protected PmsCatalogTrackEntity() {
    }

    public PmsCatalogTrackEntity(
        String id,
        String title,
        String artistName,
        String sourcePlatform,
        String primaryGenre
    ) {
        this(
            id,
            title,
            artistName,
            sourcePlatform,
            primaryGenre,
            PmsTrackAudioFeatures.unresolved()
        );
    }

    public PmsCatalogTrackEntity(
        String id,
        String title,
        String artistName,
        String sourcePlatform,
        String primaryGenre,
        PmsTrackAudioFeatures audioFeatures
    ) {
        this.id = id;
        this.title = title;
        this.artistName = artistName;
        this.sourcePlatform = sourcePlatform;
        this.primaryGenre = primaryGenre;
        this.audioFeatures = audioFeatures == null
            ? PmsTrackAudioFeatures.unresolved()
            : audioFeatures;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getSourcePlatform() {
        return sourcePlatform;
    }

    public String getPrimaryGenre() {
        return primaryGenre;
    }

    public PmsTrackAudioFeatures getAudioFeatures() {
        return audioFeatures;
    }

    public PmsTrackAudioFeatures getSpotifyAudioFeatures() {
        return audioFeatures;
    }

    public String getAudioFeatureTrackId() {
        return audioFeatures.getAudioFeatureTrackId();
    }

    public String getSpotifyTrackId() {
        return audioFeatures.getAudioFeatureTrackId();
    }

    public String getAudioFeatureSource() {
        return audioFeatures.getAudioFeatureSource();
    }

    public String getSpotifyAudioFeatureSource() {
        return audioFeatures.getAudioFeatureSource();
    }

    public boolean isAudioFeaturesFilled() {
        return audioFeatures.isComplete();
    }

    public boolean isSpotifyAudioFeaturesFilled() {
        return audioFeatures.isComplete();
    }
}
