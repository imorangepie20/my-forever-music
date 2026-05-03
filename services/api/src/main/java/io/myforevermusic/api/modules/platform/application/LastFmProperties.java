package io.myforevermusic.api.modules.platform.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.platform.lastfm")
public class LastFmProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String sharedSecret = "";
    private String apiRoot = "https://ws.audioscrobbler.com/2.0/";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret == null ? "" : sharedSecret;
    }

    public String getApiRoot() {
        return apiRoot;
    }

    public void setApiRoot(String apiRoot) {
        this.apiRoot = apiRoot == null ? "" : apiRoot;
    }

    public boolean isConfigured() {
        return enabled
            && !apiKey.isBlank()
            && !apiRoot.isBlank();
    }
}
