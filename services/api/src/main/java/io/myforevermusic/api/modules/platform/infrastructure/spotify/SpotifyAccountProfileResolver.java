package io.myforevermusic.api.modules.platform.infrastructure.spotify;

import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformAccountProfile;
import io.myforevermusic.api.modules.platform.application.PlatformAccountProfileResolver;
import io.myforevermusic.api.modules.platform.infrastructure.spotify.SpotifyWebApiClient.SpotifyUserProfile;
import org.springframework.stereotype.Component;

@Component
public class SpotifyAccountProfileResolver implements PlatformAccountProfileResolver {

    private final SpotifyWebApiClient spotifyWebApiClient;

    public SpotifyAccountProfileResolver(SpotifyWebApiClient spotifyWebApiClient) {
        this.spotifyWebApiClient = spotifyWebApiClient;
    }

    @Override
    public boolean supports(String platformId) {
        return "spotify".equals(platformId);
    }

    @Override
    public PlatformAccountProfile resolve(PlatformAccountCredential credential) {
        SpotifyUserProfile profile = spotifyWebApiClient.getCurrentUserProfile(credential);
        return new PlatformAccountProfile(
            firstNonBlank(profile.spotifyUserId(), credential.externalUserId()),
            firstNonBlank(profile.displayName(), profile.email(), credential.externalAccountLabel(), "Spotify User")
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
