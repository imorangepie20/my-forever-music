package io.myforevermusic.api.modules.platform.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.platform.oauth")
public class PlatformOAuthProperties {

    private Spotify spotify = new Spotify();

    public Spotify getSpotify() {
        return spotify;
    }

    public void setSpotify(Spotify spotify) {
        this.spotify = spotify;
    }

    public static class Spotify {

        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "http://localhost:5173/platforms/oauth/callback";
        private String authorizationUri = "https://accounts.spotify.com/authorize";
        private String tokenUri = "https://accounts.spotify.com/api/token";
        private String apiBaseUri = "https://api.spotify.com/v1";
        private List<String> scopes = List.of("user-read-email", "playlist-read-private", "playlist-read-collaborative");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId == null ? "" : clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret == null ? "" : clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri == null ? "" : redirectUri;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri == null ? "" : authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri == null ? "" : tokenUri;
        }

        public String getApiBaseUri() {
            return apiBaseUri;
        }

        public void setApiBaseUri(String apiBaseUri) {
            this.apiBaseUri = apiBaseUri == null ? "" : apiBaseUri;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes == null ? List.of() : List.copyOf(scopes);
        }

        public boolean isConfigured() {
            return enabled
                && !clientId.isBlank()
                && !redirectUri.isBlank()
                && !authorizationUri.isBlank()
                && !apiBaseUri.isBlank();
        }
    }
}
