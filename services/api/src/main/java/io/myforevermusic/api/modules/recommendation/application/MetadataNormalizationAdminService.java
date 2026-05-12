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

    /**
     * Pending candidate 중 candidate_score 가 minScore 이상인 항목을 한꺼번에 accept 한다.
     * 후속 단계의 ISRC 보강 worker 가 호출하는 같은 흐름을 운영자가 수동으로도 트리거할 수 있게 한다.
     */
    public AutoAcceptResult autoAcceptPendingCandidates(
        String adminUserId,
        double minScore,
        int batchLimit,
        String source,
        String candidateKind
    ) {
        assertAdmin(adminUserId);
        double threshold = Math.max(0.0d, Math.min(1.0d, minScore));
        int safeLimit = Math.max(1, Math.min(200, batchLimit));
        List<TrackIdentityCandidateStore.Entry> pending = candidateStore.findRecentByStatus(
            TrackIdentityCandidateStore.STATUS_PENDING,
            safeLimit
        );

        List<TrackIdentityCandidateStore.Entry> accepted = new ArrayList<>();
        List<TrackIdentityCandidateStore.Entry> skipped = new ArrayList<>();
        Instant now = Instant.now();
        String autoNote = "auto-accepted (score>=%.2f)".formatted(threshold);

        for (TrackIdentityCandidateStore.Entry candidate : pending) {
            if (!matchesFilter(candidate, source, candidateKind)) {
                continue;
            }
            Double score = candidate.candidateScore();
            if (score == null || score < threshold) {
                skipped.add(candidate);
                continue;
            }
            accepted.add(candidateStore.updateStatus(
                candidate.id(),
                TrackIdentityCandidateStore.STATUS_ACCEPTED,
                adminUserId,
                autoNote,
                now
            ));
        }
        return new AutoAcceptResult(threshold, pending.size(), accepted, skipped);
    }

    private boolean matchesFilter(
        TrackIdentityCandidateStore.Entry candidate,
        String source,
        String candidateKind
    ) {
        if (source != null && !source.isBlank() && !source.equals(candidate.source())) {
            return false;
        }
        if (candidateKind != null && !candidateKind.isBlank() && !candidateKind.equals(candidate.candidateKind())) {
            return false;
        }
        return true;
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

    public record AutoAcceptResult(
        double threshold,
        int reviewedCount,
        List<TrackIdentityCandidateStore.Entry> accepted,
        List<TrackIdentityCandidateStore.Entry> skipped
    ) {}
}
