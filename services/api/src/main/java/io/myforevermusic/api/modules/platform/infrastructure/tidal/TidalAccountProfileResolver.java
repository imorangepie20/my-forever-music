package io.myforevermusic.api.modules.platform.infrastructure.tidal;

import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformAccountProfile;
import io.myforevermusic.api.modules.platform.application.PlatformAccountProfileResolver;
import io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalUserProfile;
import org.springframework.stereotype.Component;

@Component
public class TidalAccountProfileResolver implements PlatformAccountProfileResolver {

    private final TidalWebApiClient tidalWebApiClient;

    public TidalAccountProfileResolver(TidalWebApiClient tidalWebApiClient) {
        this.tidalWebApiClient = tidalWebApiClient;
    }

    @Override
    public boolean supports(String platformId) {
        return "tidal".equals(platformId);
    }

    @Override
    public PlatformAccountProfile resolve(PlatformAccountCredential credential) {
        TidalUserProfile profile = tidalWebApiClient.getCurrentUserProfile(credential);
        return new PlatformAccountProfile(
            firstNonBlank(profile.userId(), profile.tidalUserId(), credential.externalUserId()),
            firstNonBlank(
                joinName(profile.firstName(), profile.lastName()),
                profile.email(),
                credential.externalAccountLabel(),
                "TIDAL User"
            )
        );
    }

    private String joinName(String firstName, String lastName) {
        String joined = "%s %s".formatted(
            firstName == null ? "" : firstName.trim(),
            lastName == null ? "" : lastName.trim()
        ).trim();
        return joined.isBlank() ? null : joined;
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
