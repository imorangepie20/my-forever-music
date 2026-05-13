package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryCanonicalTrackIdentityStore;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CanonicalTrackIdentityStoreTest {

    @Test
    void shouldCreateCanonicalTrackAndIdentity() {
        CanonicalTrackIdentityStore store = new InMemoryCanonicalTrackIdentityStore();

        CanonicalTrackIdentityStore.UpsertResult result = store.upsertIdentity(new CanonicalTrackIdentityStore.Draft(
            " TIDAL Track ",
            " TIDAL Artist ",
            "ISRC",
            "usrc17607839",
            "MusicBrainz",
            0.99d,
            7L,
            Instant.parse("2026-05-13T00:00:00Z")
        ));

        assertThat(result.createdCanonicalTrack()).isTrue();
        assertThat(result.createdIdentity()).isTrue();
        assertThat(result.canonicalTrack().displayTitle()).isEqualTo("TIDAL Track");
        assertThat(result.canonicalTrack().displayArtistName()).isEqualTo("TIDAL Artist");
        assertThat(result.identity().identityKind()).isEqualTo("isrc");
        assertThat(result.identity().identityValue()).isEqualTo("USRC17607839");
        assertThat(result.identity().source()).isEqualTo("musicbrainz");
        assertThat(result.identity().status()).isEqualTo(CanonicalTrackIdentityStore.STATUS_ACTIVE);
        assertThat(result.identity().createdFromCandidateId()).isEqualTo(7L);
    }

    @Test
    void shouldReturnExistingIdentityForSameNormalizedSourceKindAndValue() {
        CanonicalTrackIdentityStore store = new InMemoryCanonicalTrackIdentityStore();
        CanonicalTrackIdentityStore.UpsertResult first = store.upsertIdentity(new CanonicalTrackIdentityStore.Draft(
            "First Title",
            "First Artist",
            "isrc",
            "USRC17607839",
            "musicbrainz",
            0.98d,
            1L,
            Instant.parse("2026-05-13T00:00:00Z")
        ));

        CanonicalTrackIdentityStore.UpsertResult second = store.upsertIdentity(new CanonicalTrackIdentityStore.Draft(
            "Second Title",
            "Second Artist",
            "ISRC",
            "usrc17607839",
            "MusicBrainz",
            0.95d,
            2L,
            Instant.parse("2026-05-13T00:01:00Z")
        ));

        assertThat(second.createdCanonicalTrack()).isFalse();
        assertThat(second.createdIdentity()).isFalse();
        assertThat(second.canonicalTrack().canonicalTrackId()).isEqualTo(first.canonicalTrack().canonicalTrackId());
        assertThat(second.identity().canonicalTrackIdentityId()).isEqualTo(first.identity().canonicalTrackIdentityId());
        assertThat(store.findIdentitiesByCanonicalTrackId(first.canonicalTrack().canonicalTrackId()))
            .singleElement()
            .satisfies(identity -> assertThat(identity.identityValue()).isEqualTo("USRC17607839"));
    }

    @Test
    void shouldNormalizeMusicBrainzRecordingIdToLowercase() {
        CanonicalTrackIdentityStore store = new InMemoryCanonicalTrackIdentityStore();

        CanonicalTrackIdentityStore.UpsertResult result = store.upsertIdentity(new CanonicalTrackIdentityStore.Draft(
            "Recording",
            "Artist",
            "MBID",
            "A0B1C2D3-E4F5-6789-ABCD-EF0123456789",
            "musicbrainz",
            0.91d,
            null,
            Instant.parse("2026-05-13T00:00:00Z")
        ));

        assertThat(result.identity().identityKind()).isEqualTo("mbid");
        assertThat(result.identity().identityValue()).isEqualTo("a0b1c2d3-e4f5-6789-abcd-ef0123456789");
    }
}
