package io.myforevermusic.api.modules.pms.infrastructure.persistence;

import io.myforevermusic.api.modules.pms.application.PmsPlaylistImportStore;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pms_imported_track")
public class PmsImportedTrackEntity {

    @Id
    @Column(name = "track_id", nullable = false, length = 160)
    private String trackId;

    @Column(name = "external_track_id", nullable = false, length = 160)
    private String externalTrackId;

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

    protected PmsImportedTrackEntity() {
    }

    public PmsImportedTrackEntity(PmsPlaylistImportStore.ImportedTrackState state) {
        apply(state);
    }

    public void apply(PmsPlaylistImportStore.ImportedTrackState state) {
        this.trackId = state.trackId();
        this.externalTrackId = state.externalTrackId();
        this.title = state.title();
        this.artistName = state.artistName();
        this.sourcePlatform = state.sourcePlatform();
        this.primaryGenre = state.primaryGenre();
        this.spotifyAudioFeatures = state.spotifyAudioFeatures() == null
            ? PmsTrackSpotifyAudioFeatures.unresolved()
            : state.spotifyAudioFeatures();
    }

    public String getTrackId() {
        return trackId;
    }

    public String getExternalTrackId() {
        return externalTrackId;
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
}
