package io.myforevermusic.api.modules.ems.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ems.loose-track-playlists")
public class EmsLooseTrackPlaylistProperties {

    private boolean enabled = true;
    private int trackLimit = 5_000;
    private int tracksPerPlaylist = 40;
    private int minTrackCount = 40;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTrackLimit() {
        return trackLimit;
    }

    public void setTrackLimit(int trackLimit) {
        this.trackLimit = trackLimit;
    }

    public int getTracksPerPlaylist() {
        return tracksPerPlaylist;
    }

    public void setTracksPerPlaylist(int tracksPerPlaylist) {
        this.tracksPerPlaylist = tracksPerPlaylist;
    }

    public int getMinTrackCount() {
        return minTrackCount;
    }

    public void setMinTrackCount(int minTrackCount) {
        this.minTrackCount = minTrackCount;
    }
}
