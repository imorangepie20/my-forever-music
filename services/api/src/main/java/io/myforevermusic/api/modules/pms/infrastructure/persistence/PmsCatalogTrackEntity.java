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
    private PmsTrackSpotifyAudioFeatures spotifyAudioFeatures;

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
            PmsTrackSpotifyAudioFeatures.unresolved()
        );
    }

    public PmsCatalogTrackEntity(
        String id,
        String title,
        String artistName,
        String sourcePlatform,
        String primaryGenre,
        PmsTrackSpotifyAudioFeatures spotifyAudioFeatures
    ) {
        this.id = id;
        this.title = title;
        this.artistName = artistName;
        this.sourcePlatform = sourcePlatform;
        this.primaryGenre = primaryGenre;
        this.spotifyAudioFeatures = spotifyAudioFeatures == null
            ? PmsTrackSpotifyAudioFeatures.unresolved()
            : spotifyAudioFeatures;
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

    public PmsTrackSpotifyAudioFeatures getSpotifyAudioFeatures() {
        return spotifyAudioFeatures;
    }

    public String getSpotifyTrackId() {
        return spotifyAudioFeatures.getSpotifyTrackId();
    }

    public String getSpotifyAudioFeatureSource() {
        return spotifyAudioFeatures.getAudioFeatureSource();
    }

    public boolean isSpotifyAudioFeaturesFilled() {
        return spotifyAudioFeatures.isComplete();
    }
}
