package io.myforevermusic.api.modules.platform.infrastructure.reccobeats;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.audio-features.reccobeats")
public class ReccoBeatsProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.reccobeats.com/v1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank()
            ? "https://api.reccobeats.com/v1"
            : baseUrl;
    }
}
