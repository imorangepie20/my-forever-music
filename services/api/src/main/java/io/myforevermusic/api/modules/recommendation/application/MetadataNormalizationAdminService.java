package io.myforevermusic.api.modules.recommendation.application;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzRecordingSearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phase 2 metadata normalization 의 첫 진입 service.
 * 현재는 MusicBrainz read-only lookup 만 제공. 후속 단계로 candidate 저장 / confidence rule /
 * auto-resolve 가 추가된다.
 */
@Service
public class MetadataNormalizationAdminService {

    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";

    private final MusicBrainzClient musicBrainzClient;
    private final AuthAccountStore authAccountStore;

    public MetadataNormalizationAdminService(
        MusicBrainzClient musicBrainzClient,
        AuthAccountStore authAccountStore
    ) {
        this.musicBrainzClient = musicBrainzClient;
        this.authAccountStore = authAccountStore;
    }

    public MusicBrainzRecordingSearchResponse lookupMusicBrainz(
        String adminUserId,
        String title,
        String artist,
        int limit
    ) {
        assertAdmin(adminUserId);
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required.");
        }
        return musicBrainzClient.searchRecordings(title, artist, limit);
    }

    private void assertAdmin(String userId) {
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Metadata normalization admin access is restricted.");
        }
    }
}
