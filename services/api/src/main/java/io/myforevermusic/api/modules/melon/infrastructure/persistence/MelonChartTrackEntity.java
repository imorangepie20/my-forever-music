package io.myforevermusic.api.modules.melon.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "melon_chart_track")
public class MelonChartTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "melon_chart_track_id", nullable = false)
    private Long id;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "melon_song_id", length = 80)
    private String melonSongId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 500)
    private String artistName;

    @Column(name = "album_title", length = 500)
    private String albumTitle;

    @Column(name = "image_url", length = 800)
    private String imageUrl;

    @Column(name = "song_external_url", length = 800)
    private String songExternalUrl;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    protected MelonChartTrackEntity() {}

    public MelonChartTrackEntity(
        int rank,
        String melonSongId,
        String title,
        String artistName,
        String albumTitle,
        String imageUrl,
        String songExternalUrl,
        Instant snapshotAt
    ) {
        this.rank = rank;
        this.melonSongId = melonSongId;
        this.title = title;
        this.artistName = artistName;
        this.albumTitle = albumTitle;
        this.imageUrl = imageUrl;
        this.songExternalUrl = songExternalUrl;
        this.snapshotAt = snapshotAt;
    }

    public Long getId() { return id; }
    public int getRank() { return rank; }
    public String getMelonSongId() { return melonSongId; }
    public String getTitle() { return title; }
    public String getArtistName() { return artistName; }
    public String getAlbumTitle() { return albumTitle; }
    public String getImageUrl() { return imageUrl; }
    public String getSongExternalUrl() { return songExternalUrl; }
    public Instant getSnapshotAt() { return snapshotAt; }
}
