package io.myforevermusic.api.modules.recommendation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzRecording;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient.MusicBrainzRecordingSearchResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phase 2 metadata normalization admin service.
 * 1차: MusicBrainz read-only lookup.
 * 2차(현재): lookup 결과를 track_identity_candidate 에 저장하고 운영자가 accept/reject 한다.
 */
@Service
public class MetadataNormalizationAdminService {

    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";

    private final MusicBrainzClient musicBrainzClient;
    private final TrackIdentityCandidateStore candidateStore;
    private final AuthAccountStore authAccountStore;
    private final ObjectMapper objectMapper;

    public MetadataNormalizationAdminService(
        MusicBrainzClient musicBrainzClient,
        TrackIdentityCandidateStore candidateStore,
        AuthAccountStore authAccountStore,
        ObjectMapper objectMapper
    ) {
        this.musicBrainzClient = musicBrainzClient;
        this.candidateStore = candidateStore;
        this.authAccountStore = authAccountStore;
        this.objectMapper = objectMapper;
    }

    public LookupResult lookupMusicBrainz(
        String adminUserId,
        String title,
        String artist,
        int limit,
        boolean persistCandidates
    ) {
        assertAdmin(adminUserId);
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required.");
        }
        MusicBrainzRecordingSearchResponse response = musicBrainzClient.searchRecordings(title, artist, limit);
        List<TrackIdentityCandidateStore.Entry> savedCandidates = new ArrayList<>();
        if (persistCandidates) {
            Instant now = Instant.now();
            for (MusicBrainzRecording recording : Optional.ofNullable(response.recordings()).orElse(List.of())) {
                if (recording.id() == null || recording.id().isBlank()) {
                    continue;
                }
                savedCandidates.add(candidateStore.save(new TrackIdentityCandidateStore.Draft(
                    title,
                    artist,
                    "musicbrainz",
                    "mbid",
                    recording.id(),
                    normalizeScore(recording.score()),
                    serializeMetadata(recording),
                    adminUserId,
                    now
                )));
                if (recording.isrcs() != null) {
                    for (String isrc : recording.isrcs()) {
                        if (isrc == null || isrc.isBlank()) {
                            continue;
                        }
                        savedCandidates.add(candidateStore.save(new TrackIdentityCandidateStore.Draft(
                            title,
                            artist,
                            "musicbrainz",
                            "isrc",
                            isrc,
                            normalizeScore(recording.score()),
                            serializeMetadata(recording),
                            adminUserId,
                            now
                        )));
                    }
                }
            }
        }
        return new LookupResult(response, savedCandidates);
    }

    public List<TrackIdentityCandidateStore.Entry> listCandidates(
        String adminUserId,
        String status,
        int limit
    ) {
        assertAdmin(adminUserId);
        return candidateStore.findRecentByStatus(status, limit);
    }

    public TrackIdentityCandidateStore.Entry acceptCandidate(
        String adminUserId,
        Long candidateId,
        String notes
    ) {
        assertAdmin(adminUserId);
        return candidateStore.updateStatus(
            candidateId,
            TrackIdentityCandidateStore.STATUS_ACCEPTED,
            adminUserId,
            notes,
            Instant.now()
        );
    }

    public TrackIdentityCandidateStore.Entry rejectCandidate(
        String adminUserId,
        Long candidateId,
        String notes
    ) {
        assertAdmin(adminUserId);
        return candidateStore.updateStatus(
            candidateId,
            TrackIdentityCandidateStore.STATUS_REJECTED,
            adminUserId,
            notes,
            Instant.now()
        );
    }

    private Double normalizeScore(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.min(1.0d, Math.max(0.0d, score / 100.0d));
    }

    private String serializeMetadata(MusicBrainzRecording recording) {
        try {
            return objectMapper.writeValueAsString(recording);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void assertAdmin(String userId) {
        String normalizedEmail = authAccountStore.findByUserId(userId)
            .map(account -> account.normalizedEmail())
            .orElse("");
        if (!ADMIN_EMAIL.equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Metadata normalization admin access is restricted.");
        }
    }

    public record LookupResult(
        MusicBrainzRecordingSearchResponse response,
        List<TrackIdentityCandidateStore.Entry> savedCandidates
    ) {}
}
