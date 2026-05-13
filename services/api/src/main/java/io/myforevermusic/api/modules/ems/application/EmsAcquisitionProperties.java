package io.myforevermusic.api.modules.ems.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ems.acquisition")
public class EmsAcquisitionProperties {

    private boolean enabled = true;
    private String userId = "";
    private List<String> platforms = List.of("spotify", "tidal");
    private List<Source> sources = List.of();
    private int maxArticlesPerSource = 20;
    private int maxSignalsPerRun = 40;
    private int perSeedLimit = 5;

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

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public int getMaxArticlesPerSource() {
        return maxArticlesPerSource;
    }

    public void setMaxArticlesPerSource(int maxArticlesPerSource) {
        this.maxArticlesPerSource = maxArticlesPerSource;
    }

    public int getMaxSignalsPerRun() {
        return maxSignalsPerRun;
    }

    public void setMaxSignalsPerRun(int maxSignalsPerRun) {
        this.maxSignalsPerRun = maxSignalsPerRun;
    }

    public int getPerSeedLimit() {
        return perSeedLimit;
    }

    public void setPerSeedLimit(int perSeedLimit) {
        this.perSeedLimit = perSeedLimit;
    }

    public static class Source {
        private boolean enabled = true;
        private String type = "rss";
        private String name = "";
        private String url = "";
        private double weight = 1.0d;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }
}
