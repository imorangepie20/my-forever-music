package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsCollectedTrackRepository;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsImportedTrackEntity;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsImportedTrackRepository;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsUserTrackEntity;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsUserTrackRepository;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryCanonicalTrackIdentityStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryTrackIdentityCandidateAuditStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryTrackIdentityCandidateStore;
import io.myforevermusic.api.modules.recommendation.infrastructure.discogs.DiscogsClient;
import io.myforevermusic.api.modules.recommendation.infrastructure.discogs.DiscogsClient.DiscogsMasterDetail;
import io.myforevermusic.api.modules.recommendation.infrastructure.discogs.DiscogsClient.DiscogsReleaseDetail;
import io.myforevermusic.api.modules.recommendation.infrastructure.discogs.DiscogsClient.DiscogsReleaseLabel;
import io.myforevermusic.api.modules.recommendation.infrastructure.discogs.DiscogsClient.DiscogsSearchResponse;
import io.myforevermusic.api.modules.recommendation.infrastructure.discogs.DiscogsClient.DiscogsSearchResult;
import io.myforevermusic.api.modules.recommendation.infrastructure.musicbrainz.MusicBrainzClient;
import io.myforevermusic.api.modules.recommendation.infrastructure.wikidata.WikidataClient;
import io.myforevermusic.api.modules.recommendation.infrastructure.wikidata.WikidataClient.WikidataEntitySearchResponse;
import io.myforevermusic.api.modules.recommendation.infrastructure.wikidata.WikidataClient.WikidataEntitySearchResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MetadataNormalizationAdminServiceTest {

    private static final String ADMIN_USER_ID = "user-admin";
    private static final String ISRC = "USRC17607839";

    private InMemoryTrackIdentityCandidateStore candidateStore;
    private InMemoryTrackIdentityCandidateAuditStore auditStore;
    private InMemoryCanonicalTrackIdentityStore canonicalTrackIdentityStore;
    private WikidataClient wikidataClient;
    private DiscogsClient discogsClient;
    private EmsCollectedTrackRepository trackRepository;
    private PmsImportedTrackRepository pmsImportedTrackRepository;
    private PmsUserTrackRepository pmsUserTrackRepository;
    private MetadataNormalizationAdminService service;

    @BeforeEach
    void setUp() {
        candidateStore = new InMemoryTrackIdentityCandidateStore();
        auditStore = new InMemoryTrackIdentityCandidateAuditStore();
        canonicalTrackIdentityStore = new InMemoryCanonicalTrackIdentityStore();
        wikidataClient = mock(WikidataClient.class);
        discogsClient = mock(DiscogsClient.class);
        trackRepository = mock(EmsCollectedTrackRepository.class);
        pmsImportedTrackRepository = mock(PmsImportedTrackRepository.class);
        pmsUserTrackRepository = mock(PmsUserTrackRepository.class);
        AuthAccountStore authAccountStore = mock(AuthAccountStore.class);
        when(authAccountStore.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminAccount()));
        service = new MetadataNormalizationAdminService(
            mock(MusicBrainzClient.class),
            wikidataClient,
            discogsClient,
            candidateStore,
            auditStore,
            canonicalTrackIdentityStore,
            authAccountStore,
            new ObjectMapper(),
            trackRepository,
            pmsImportedTrackRepository,
            pmsUserTrackRepository,
            new CandidateQualityScorer()
        );
    }

    @Test
    void shouldApplyAcceptedIsrcCandidateToTracksWithoutIsrc() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        EmsCollectedTrackEntity trackWithNullIsrc = track(10L, "tidal-track-001", "TIDAL Track", "TIDAL Artist", null);
        EmsCollectedTrackEntity trackWithBlankIsrc = track(11L, "tidal-track-002", "TIDAL Track", "TIDAL Artist", "");

        when(trackRepository.findByTitleIgnoreCaseAndArtistNameIgnoreCase("TIDAL Track", "TIDAL Artist"))
            .thenReturn(List.of(trackWithNullIsrc, trackWithBlankIsrc));
        when(trackRepository.updateIsrcIfNull(10L, ISRC)).thenReturn(1);
        when(trackRepository.updateIsrcIfNull(11L, ISRC)).thenReturn(1);

        MetadataNormalizationAdminService.IsrcApplyResult result =
            service.applyAcceptedIsrcCandidates(ADMIN_USER_ID, 100);

        assertThat(result.reviewedCount()).isEqualTo(1);
        assertThat(result.isrcConsideredCount()).isEqualTo(1);
        assertThat(result.applied()).singleElement().satisfies(applied -> {
            assertThat(applied.candidate().id()).isEqualTo(accepted.id());
            assertThat(applied.candidate().status()).isEqualTo(TrackIdentityCandidateStore.STATUS_APPLIED);
            assertThat(applied.updatedTrackIds()).containsExactly(10L, 11L);
            assertThat(applied.conflictTrackIds()).isEmpty();
        });
        assertThat(result.noMatch()).isEmpty();
        assertThat(result.conflicts()).isEmpty();
        assertThat(auditStore.findByCandidateIdAndAction(accepted.id(), TrackIdentityCandidateAuditStore.ACTION_APPLY))
            .extracting(TrackIdentityCandidateAuditStore.Entry::emsCollectedTrackId)
            .containsExactly(10L, 11L);
        verify(trackRepository).updateIsrcIfNull(10L, ISRC);
        verify(trackRepository).updateIsrcIfNull(11L, ISRC);
    }

    @Test
    void shouldNotOverwriteConflictingIsrc() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        EmsCollectedTrackEntity conflictingTrack = track(
            20L,
            "tidal-track-003",
            "TIDAL Track",
            "TIDAL Artist",
            "GBUM71029604"
        );

        when(trackRepository.findByTitleIgnoreCaseAndArtistNameIgnoreCase("TIDAL Track", "TIDAL Artist"))
            .thenReturn(List.of(conflictingTrack));

        MetadataNormalizationAdminService.IsrcApplyResult result =
            service.applyAcceptedIsrcCandidates(ADMIN_USER_ID, 100);

        assertThat(result.applied()).isEmpty();
        assertThat(result.noMatch()).isEmpty();
        assertThat(result.conflicts()).singleElement().satisfies(conflict -> {
            assertThat(conflict.id()).isEqualTo(accepted.id());
            assertThat(conflict.status()).isEqualTo(TrackIdentityCandidateStore.STATUS_CONFLICT);
            assertThat(conflict.notes()).contains("20");
        });
        assertThat(auditStore.findByCandidateIdAndAction(accepted.id(), TrackIdentityCandidateAuditStore.ACTION_CONFLICT))
            .extracting(TrackIdentityCandidateAuditStore.Entry::emsCollectedTrackId)
            .containsExactly(20L);
        verifyNoInteractionsForUpdate();
    }

    @Test
    void shouldMarkAcceptedIsrcCandidateNoMatchWhenNoTrackMatchesQuery() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("Missing Track", "Missing Artist", ISRC);

        when(trackRepository.findByTitleIgnoreCaseAndArtistNameIgnoreCase("Missing Track", "Missing Artist"))
            .thenReturn(List.of());

        MetadataNormalizationAdminService.IsrcApplyResult result =
            service.applyAcceptedIsrcCandidates(ADMIN_USER_ID, 100);

        assertThat(result.applied()).isEmpty();
        assertThat(result.conflicts()).isEmpty();
        assertThat(result.noMatch()).singleElement().satisfies(noMatch -> {
            assertThat(noMatch.id()).isEqualTo(accepted.id());
            assertThat(noMatch.status()).isEqualTo(TrackIdentityCandidateStore.STATUS_NO_MATCH);
        });
        assertThat(auditStore.findByCandidateIdAndAction(accepted.id(), TrackIdentityCandidateAuditStore.ACTION_NO_MATCH))
            .hasSize(1);
        verifyNoInteractionsForUpdate();
    }

    @Test
    void shouldRollbackAppliedIsrcOnlyFromRecordedTrackIdsWhenCurrentValueMatches() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        EmsCollectedTrackEntity track = track(30L, "tidal-track-004", "TIDAL Track", "TIDAL Artist", null);
        when(trackRepository.findByTitleIgnoreCaseAndArtistNameIgnoreCase("TIDAL Track", "TIDAL Artist"))
            .thenReturn(List.of(track));
        when(trackRepository.updateIsrcIfNull(30L, ISRC)).thenReturn(1);
        service.applyAcceptedIsrcCandidates(ADMIN_USER_ID, 100);
        when(trackRepository.clearIsrcIfMatches(30L, ISRC)).thenReturn(1);

        MetadataNormalizationAdminService.IsrcRollbackResult result =
            service.rollbackAppliedIsrcCandidate(ADMIN_USER_ID, accepted.id(), "wrong candidate");

        assertThat(result.candidate().status()).isEqualTo(TrackIdentityCandidateStore.STATUS_ROLLED_BACK);
        assertThat(result.targetTrackIds()).containsExactly(30L);
        assertThat(result.clearedTrackIds()).containsExactly(30L);
        assertThat(result.skippedTrackIds()).isEmpty();
        assertThat(result.candidate().notes()).contains("wrong candidate");
        verify(trackRepository).clearIsrcIfMatches(30L, ISRC);
    }

    @Test
    void shouldListCandidateAuditEntries() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        EmsCollectedTrackEntity track = track(35L, "tidal-track-005", "TIDAL Track", "TIDAL Artist", null);
        when(trackRepository.findByTitleIgnoreCaseAndArtistNameIgnoreCase("TIDAL Track", "TIDAL Artist"))
            .thenReturn(List.of(track));
        when(trackRepository.updateIsrcIfNull(35L, ISRC)).thenReturn(1);
        service.applyAcceptedIsrcCandidates(ADMIN_USER_ID, 100);

        MetadataNormalizationAdminService.CandidateAuditResult result =
            service.listCandidateAudit(ADMIN_USER_ID, accepted.id());

        assertThat(result.candidate().id()).isEqualTo(accepted.id());
        assertThat(result.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.action()).isEqualTo(TrackIdentityCandidateAuditStore.ACTION_APPLY);
            assertThat(entry.emsCollectedTrackId()).isEqualTo(35L);
            assertThat(entry.status()).isEqualTo(TrackIdentityCandidateAuditStore.STATUS_APPLIED);
        });
    }

    @Test
    void shouldPersistWikidataIdentityCandidates() {
        when(wikidataClient.searchEntities("TIDAL Track", "TIDAL Artist", 10))
            .thenReturn(new WikidataEntitySearchResponse(List.of(
                new WikidataEntitySearchResult(
                    "Q12345",
                    "Q12345",
                    "TIDAL Track",
                    "song by TIDAL Artist",
                    "https://www.wikidata.org/wiki/Q12345"
                )
            )));

        MetadataNormalizationAdminService.ExternalLookupResult result =
            service.lookupWikidata(ADMIN_USER_ID, "TIDAL Track", "TIDAL Artist", 10, true);

        assertThat(result.source()).isEqualTo("wikidata");
        assertThat(result.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.candidateKind()).isEqualTo("wikidata_qid");
            assertThat(candidate.candidateValue()).isEqualTo("Q12345");
        });
        assertThat(result.savedCandidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.source()).isEqualTo("wikidata");
            assertThat(candidate.candidateKind()).isEqualTo("wikidata_qid");
            assertThat(candidate.candidateValue()).isEqualTo("Q12345");
            assertThat(candidate.status()).isEqualTo(TrackIdentityCandidateStore.STATUS_PENDING);
        });
    }

    @Test
    void shouldPersistDiscogsMasterIdentityCandidates() {
        when(discogsClient.searchMasters("TIDAL Track", "TIDAL Artist", 10))
            .thenReturn(new DiscogsSearchResponse(List.of(
                new DiscogsSearchResult(
                    7654321,
                    "master",
                    "TIDAL Artist - TIDAL Track",
                    "US",
                    "2020",
                    "https://api.discogs.com/masters/7654321"
                )
            )));

        MetadataNormalizationAdminService.ExternalLookupResult result =
            service.lookupDiscogsMasters(ADMIN_USER_ID, "TIDAL Track", "TIDAL Artist", 10, true);

        assertThat(result.source()).isEqualTo("discogs");
        assertThat(result.savedCandidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.source()).isEqualTo("discogs");
            assertThat(candidate.candidateKind()).isEqualTo("discogs_master_id");
            assertThat(candidate.candidateValue()).isEqualTo("7654321");
        });
    }

    @Test
    void shouldPromoteAppliedCandidateToCanonicalIdentity() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        TrackIdentityCandidateStore.Entry applied = candidateStore.updateStatus(
            accepted.id(),
            TrackIdentityCandidateStore.STATUS_APPLIED,
            ADMIN_USER_ID,
            "already applied",
            Instant.parse("2026-05-12T00:02:00Z")
        );
        when(trackRepository.linkCanonicalTrackByIsrc(ISRC, 1L)).thenReturn(1);
        when(pmsImportedTrackRepository.linkCanonicalTrackByIsrc(ISRC, 1L)).thenReturn(2);
        when(pmsUserTrackRepository.linkCanonicalTrackByIsrc(ISRC, 1L)).thenReturn(3);
        when(pmsUserTrackRepository.countCanonicalTrackConflictsByIsrc(ISRC, 1L)).thenReturn(1L);

        MetadataNormalizationAdminService.CanonicalPromotionResult result =
            service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, applied.id());

        assertThat(result.candidate().id()).isEqualTo(applied.id());
        assertThat(result.createdCanonicalTrack()).isTrue();
        assertThat(result.createdIdentity()).isTrue();
        assertThat(result.canonicalTrack().displayTitle()).isEqualTo("TIDAL Track");
        assertThat(result.canonicalTrack().displayArtistName()).isEqualTo("TIDAL Artist");
        assertThat(result.identity().identityKind()).isEqualTo("isrc");
        assertThat(result.identity().identityValue()).isEqualTo(ISRC);
        assertThat(result.identity().source()).isEqualTo("musicbrainz");
        assertThat(result.identity().createdFromCandidateId()).isEqualTo(applied.id());
        assertThat(result.links().emsLinkedCount()).isEqualTo(1);
        assertThat(result.links().pmsImportedLinkedCount()).isEqualTo(2);
        assertThat(result.links().pmsUserLinkedCount()).isEqualTo(3);
        assertThat(result.links().totalConflictCount()).isEqualTo(1);
        assertThat(auditStore.findByCandidateIdAndAction(applied.id(), TrackIdentityCandidateAuditStore.ACTION_CANONICAL_PROMOTE))
            .singleElement()
            .satisfies(entry -> {
                assertThat(entry.status()).isEqualTo(TrackIdentityCandidateAuditStore.STATUS_CANONICAL_PROMOTED);
                assertThat(entry.message()).contains("canonical track id");
                assertThat(entry.message()).contains("linked rows ems=1");
            });
    }

    @Test
    void shouldReuseExistingCanonicalIdentityOnDuplicatePromotion() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        TrackIdentityCandidateStore.Entry applied = candidateStore.updateStatus(
            accepted.id(),
            TrackIdentityCandidateStore.STATUS_APPLIED,
            ADMIN_USER_ID,
            "already applied",
            Instant.parse("2026-05-12T00:02:00Z")
        );

        MetadataNormalizationAdminService.CanonicalPromotionResult first =
            service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, applied.id());
        MetadataNormalizationAdminService.CanonicalPromotionResult second =
            service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, applied.id());

        assertThat(second.createdCanonicalTrack()).isFalse();
        assertThat(second.createdIdentity()).isFalse();
        assertThat(second.canonicalTrack().canonicalTrackId()).isEqualTo(first.canonicalTrack().canonicalTrackId());
        assertThat(second.identity().canonicalTrackIdentityId()).isEqualTo(first.identity().canonicalTrackIdentityId());
        assertThat(auditStore.findByCandidateIdAndAction(applied.id(), TrackIdentityCandidateAuditStore.ACTION_CANONICAL_PROMOTE))
            .extracting(TrackIdentityCandidateAuditStore.Entry::status)
            .containsExactly(
                TrackIdentityCandidateAuditStore.STATUS_CANONICAL_PROMOTED,
                TrackIdentityCandidateAuditStore.STATUS_CANONICAL_EXISTS
            );
    }

    @Test
    void shouldRejectCanonicalPromotionForNonAppliedCandidate() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);

        assertThatThrownBy(() -> service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, accepted.id()))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .hasMessageContaining("Only applied ISRC candidates");
    }

    @Test
    void shouldPromoteAcceptedWikidataCandidateToCanonicalIdentity() {
        TrackIdentityCandidateStore.Entry candidate = candidateStore.save(new TrackIdentityCandidateStore.Draft(
            "TIDAL Track",
            "TIDAL Artist",
            "wikidata",
            "wikidata_qid",
            "q12345",
            null,
            "{}",
            ADMIN_USER_ID,
            Instant.parse("2026-05-12T00:00:00Z")
        ));
        TrackIdentityCandidateStore.Entry accepted = candidateStore.updateStatus(
            candidate.id(),
            TrackIdentityCandidateStore.STATUS_ACCEPTED,
            ADMIN_USER_ID,
            "accepted external identity",
            Instant.parse("2026-05-12T00:01:00Z")
        );

        MetadataNormalizationAdminService.CanonicalPromotionResult result =
            service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, accepted.id());

        assertThat(result.identity().source()).isEqualTo("wikidata");
        assertThat(result.identity().identityKind()).isEqualTo("wikidata_qid");
        assertThat(result.identity().identityValue()).isEqualTo("Q12345");
        assertThat(result.links().emsLinkedCount()).isZero();
    }

    @Test
    void shouldPopulateReleaseContextWhenPromotingDiscogsCandidate() {
        when(discogsClient.getMaster(12345)).thenReturn(new DiscogsMasterDetail(
            12345,
            "Queen - Bohemian Rhapsody",
            1975,
            67890,
            "https://api.discogs.com/releases/67890"
        ));
        when(discogsClient.getRelease(67890)).thenReturn(new DiscogsReleaseDetail(
            67890,
            "Queen - Bohemian Rhapsody",
            "UK",
            "1975",
            List.of(new DiscogsReleaseLabel(
                123,
                "EMI",
                "EMI 2375",
                "https://api.discogs.com/labels/123"
            ))
        ));
        TrackIdentityCandidateStore.Entry candidate = candidateStore.save(new TrackIdentityCandidateStore.Draft(
            "Bohemian Rhapsody",
            "Queen",
            "discogs",
            "discogs_master_id",
            "12345",
            0.95d,
            "{\"id\":12345,\"type\":\"master\",\"title\":\"Queen - Bohemian Rhapsody\",\"country\":\"UK\",\"year\":\"1975\"}",
            ADMIN_USER_ID,
            Instant.parse("2026-05-12T00:00:00Z")
        ));
        TrackIdentityCandidateStore.Entry accepted = candidateStore.updateStatus(
            candidate.id(),
            TrackIdentityCandidateStore.STATUS_ACCEPTED,
            ADMIN_USER_ID,
            "accepted",
            Instant.parse("2026-05-12T00:01:00Z")
        );

        MetadataNormalizationAdminService.CanonicalPromotionResult result =
            service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, accepted.id());

        assertThat(result.canonicalTrack().releaseYear()).isEqualTo("1975");
        assertThat(result.canonicalTrack().releaseCountry()).isEqualTo("UK");
        assertThat(result.canonicalTrack().releaseLabel()).isEqualTo("EMI");
    }

    @Test
    void shouldPropagateDiscogsDetailFailureDuringLabelEnrichment() {
        when(discogsClient.getMaster(12345))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Discogs responded with 502"
            ));
        TrackIdentityCandidateStore.Entry candidate = candidateStore.save(new TrackIdentityCandidateStore.Draft(
            "Bohemian Rhapsody",
            "Queen",
            "discogs",
            "discogs_master_id",
            "12345",
            0.95d,
            "{\"id\":12345,\"type\":\"master\",\"title\":\"Queen - Bohemian Rhapsody\",\"country\":\"UK\",\"year\":\"1975\"}",
            ADMIN_USER_ID,
            Instant.parse("2026-05-12T00:00:00Z")
        ));
        TrackIdentityCandidateStore.Entry accepted = candidateStore.updateStatus(
            candidate.id(),
            TrackIdentityCandidateStore.STATUS_ACCEPTED,
            ADMIN_USER_ID,
            "accepted",
            Instant.parse("2026-05-12T00:01:00Z")
        );

        assertThatThrownBy(() -> service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, accepted.id()))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .hasMessageContaining("Discogs responded with 502");

        assertThat(canonicalTrackIdentityStore.findActiveIdentity("discogs", "discogs_master_id", "12345"))
            .isEmpty();
    }

    @Test
    void shouldListCanonicalLinkConflictsForPromotedIsrcCandidate() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        TrackIdentityCandidateStore.Entry applied = candidateStore.updateStatus(
            accepted.id(),
            TrackIdentityCandidateStore.STATUS_APPLIED,
            ADMIN_USER_ID,
            "already applied",
            Instant.parse("2026-05-12T00:02:00Z")
        );
        service.promoteCandidateToCanonicalIdentity(ADMIN_USER_ID, applied.id());
        EmsCollectedTrackEntity emsConflict = track(
            50L,
            "tidal-conflict-001",
            "Conflict Track",
            "Conflict Artist",
            ISRC
        );
        ReflectionTestUtils.setField(emsConflict, "canonicalTrackId", 99L);
        PmsImportedTrackEntity importedConflict = importedTrack("imported-001", "spotify-conflict-001", 88L);
        PmsUserTrackEntity userConflict = userTrack("user-001", "tidal-conflict-002", 77L);
        when(trackRepository.findCanonicalTrackConflictsByIsrc(ISRC, 1L)).thenReturn(List.of(emsConflict));
        when(pmsImportedTrackRepository.findCanonicalTrackConflictsByIsrc(ISRC, 1L))
            .thenReturn(List.of(importedConflict));
        when(pmsUserTrackRepository.findCanonicalTrackConflictsByIsrc(ISRC, 1L)).thenReturn(List.of(userConflict));

        MetadataNormalizationAdminService.CanonicalLinkConflictResult result =
            service.listCanonicalLinkConflicts(ADMIN_USER_ID, applied.id());

        assertThat(result.targetIdentity().canonicalTrackId()).isEqualTo(1L);
        assertThat(result.rows()).hasSize(3);
        assertThat(result.rows())
            .extracting(MetadataNormalizationAdminService.CanonicalLinkConflictRow::trackStore)
            .containsExactly("ems_collected_track", "pms_imported_track", "pms_user_track");
        assertThat(result.rows())
            .extracting(MetadataNormalizationAdminService.CanonicalLinkConflictRow::existingCanonicalTrackId)
            .containsExactly(99L, 88L, 77L);
    }

    @Test
    void shouldMarkRollbackReviewRequiredWhenRecordedTrackIdsCannotBeCleared() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        markAppliedWithAudit(
            accepted.id(),
            40L
        );
        when(trackRepository.clearIsrcIfMatches(40L, ISRC)).thenReturn(0);

        MetadataNormalizationAdminService.IsrcRollbackResult result =
            service.rollbackAppliedIsrcCandidate(ADMIN_USER_ID, accepted.id(), null);

        assertThat(result.candidate().status()).isEqualTo(TrackIdentityCandidateStore.STATUS_REVIEW_REQUIRED);
        assertThat(result.targetTrackIds()).containsExactly(40L);
        assertThat(result.clearedTrackIds()).isEmpty();
        assertThat(result.skippedTrackIds()).containsExactly(40L);
        assertThat(auditStore.findByCandidateIdAndAction(accepted.id(), TrackIdentityCandidateAuditStore.ACTION_ROLLBACK))
            .singleElement()
            .satisfies(entry -> {
                assertThat(entry.emsCollectedTrackId()).isEqualTo(40L);
                assertThat(entry.status()).isEqualTo(TrackIdentityCandidateAuditStore.STATUS_REVIEW_REQUIRED);
            });
        verify(trackRepository).clearIsrcIfMatches(40L, ISRC);
    }

    @Test
    void shouldMarkRollbackReviewRequiredWhenAppliedTrackIdsAreMissing() {
        TrackIdentityCandidateStore.Entry accepted = acceptedIsrcCandidate("TIDAL Track", "TIDAL Artist", ISRC);
        candidateStore.updateStatus(
            accepted.id(),
            TrackIdentityCandidateStore.STATUS_APPLIED,
            ADMIN_USER_ID,
            "already applied (matching ISRCs)",
            Instant.parse("2026-05-12T00:02:00Z")
        );

        MetadataNormalizationAdminService.IsrcRollbackResult result =
            service.rollbackAppliedIsrcCandidate(ADMIN_USER_ID, accepted.id(), null);

        assertThat(result.candidate().status()).isEqualTo(TrackIdentityCandidateStore.STATUS_REVIEW_REQUIRED);
        assertThat(result.targetTrackIds()).isEmpty();
        assertThat(result.clearedTrackIds()).isEmpty();
        assertThat(result.skippedTrackIds()).isEmpty();
    }

    private void markAppliedWithAudit(Long candidateId, Long trackId) {
        candidateStore.updateStatus(
            candidateId,
            TrackIdentityCandidateStore.STATUS_APPLIED,
            ADMIN_USER_ID,
            "applied using structured audit",
            Instant.parse("2026-05-12T00:02:00Z")
        );
        auditStore.save(new TrackIdentityCandidateAuditStore.Draft(
            candidateId,
            TrackIdentityCandidateAuditStore.ACTION_APPLY,
            trackId,
            ISRC,
            null,
            ISRC,
            TrackIdentityCandidateAuditStore.STATUS_APPLIED,
            "applied accepted ISRC candidate",
            ADMIN_USER_ID,
            Instant.parse("2026-05-12T00:02:00Z")
        ));
    }

    private TrackIdentityCandidateStore.Entry acceptedIsrcCandidate(String title, String artist, String isrc) {
        TrackIdentityCandidateStore.Entry candidate = candidateStore.save(new TrackIdentityCandidateStore.Draft(
            title,
            artist,
            "musicbrainz",
            "isrc",
            isrc,
            0.99d,
            "{}",
            ADMIN_USER_ID,
            Instant.parse("2026-05-12T00:00:00Z")
        ));
        return candidateStore.updateStatus(
            candidate.id(),
            TrackIdentityCandidateStore.STATUS_ACCEPTED,
            ADMIN_USER_ID,
            "accepted for apply test",
            Instant.parse("2026-05-12T00:01:00Z")
        );
    }

    private EmsCollectedTrackEntity track(
        Long id,
        String externalTrackId,
        String title,
        String artistName,
        String isrc
    ) {
        EmsCollectedTrackEntity track = new EmsCollectedTrackEntity(
            externalTrackId,
            title,
            artistName,
            "tidal",
            isrc,
            null,
            null,
            null,
            null,
            null,
            180000,
            "search_pool",
            Instant.parse("2026-05-09T00:00:00Z"),
            null
        );
        ReflectionTestUtils.setField(track, "id", id);
        return track;
    }

    private PmsImportedTrackEntity importedTrack(String trackId, String externalTrackId, Long canonicalTrackId) {
        PmsImportedTrackEntity track = mock(PmsImportedTrackEntity.class);
        when(track.getTrackId()).thenReturn(trackId);
        when(track.getExternalTrackId()).thenReturn(externalTrackId);
        when(track.getTitle()).thenReturn("Imported Conflict Track");
        when(track.getArtistName()).thenReturn("Imported Conflict Artist");
        when(track.getSourcePlatform()).thenReturn("spotify");
        when(track.getIsrc()).thenReturn(ISRC);
        when(track.getCanonicalTrackId()).thenReturn(canonicalTrackId);
        return track;
    }

    private PmsUserTrackEntity userTrack(String trackId, String externalTrackId, Long canonicalTrackId) {
        PmsUserTrackEntity track = mock(PmsUserTrackEntity.class);
        when(track.getTrackId()).thenReturn(trackId);
        when(track.getExternalTrackId()).thenReturn(externalTrackId);
        when(track.getTitle()).thenReturn("User Conflict Track");
        when(track.getArtistName()).thenReturn("User Conflict Artist");
        when(track.getSourcePlatform()).thenReturn("tidal");
        when(track.getIsrc()).thenReturn(ISRC);
        when(track.getCanonicalTrackId()).thenReturn(canonicalTrackId);
        return track;
    }

    private AuthRegisteredAccount adminAccount() {
        return new AuthRegisteredAccount(
            ADMIN_USER_ID,
            "jowoosungtidal@gmail.com",
            "jowoosungtidal@gmail.com",
            "Admin",
            "tidal",
            null,
            null,
            false,
            "registered",
            Instant.parse("2026-05-09T00:00:00Z"),
            Instant.parse("2026-05-09T00:00:00Z"),
            Instant.parse("2026-05-09T00:00:00Z")
        );
    }

    private void verifyNoInteractionsForUpdate() {
        verify(trackRepository, never()).updateIsrcIfNull(anyLong(), anyString());
    }
}
