package io.myforevermusic.api.modules.ems.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ems.discovery")
public class EmsDiscoveryProperties {

    private boolean enabled = true;
    private String userId = "";
    private List<String> platforms = List.of("tidal", "spotify");
    private List<String> seedQueries = List.of(
        "tidal:THE_HITS",
        "tidal:POPULAR_MIXES",
        "tidal:POPULAR_PLAYLISTS",
        "tidal:FROM_OUR_EDITORS",
        "spotify:featured-playlists",
        "spotify:category:toplists"
    );
    private int perQueryLimit = 5;
    private int displayLimit = 12;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<String> platforms) {
        this.platforms = platforms;
    }

    public List<String> getSeedQueries() {
        return seedQueries;
    }

    public void setSeedQueries(List<String> seedQueries) {
        this.seedQueries = seedQueries;
    }

    public int getPerQueryLimit() {
        return perQueryLimit;
    }

    public void setPerQueryLimit(int perQueryLimit) {
        this.perQueryLimit = perQueryLimit;
    }

    public int getDisplayLimit() {
        return displayLimit;
    }

    public void setDisplayLimit(int displayLimit) {
        this.displayLimit = displayLimit;
    }
}
