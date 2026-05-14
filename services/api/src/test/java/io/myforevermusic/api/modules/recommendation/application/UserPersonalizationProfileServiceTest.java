package io.myforevermusic.api.modules.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.auth.application.AuthAccountStore;
import io.myforevermusic.api.modules.auth.application.AuthRegisteredAccount;
import io.myforevermusic.api.modules.recommendation.application.UserMusicEventStore.StoredEvent;
import io.myforevermusic.api.modules.recommendation.infrastructure.local.InMemoryUserPersonalizationProfileStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class UserPersonalizationProfileServiceTest {

    private static final String ADMIN_USER_ID = "admin-001";
    private static final String ADMIN_EMAIL = "jowoosungtidal@gmail.com";
    private static final String TARGET_USER_ID = "user-target-1";

    private AuthAccountStore authAccountStore;
    private UserMusicEventStore eventStore;
    private InMemoryUserPersonalizationProfileStore profileStore;
    private UserPersonalizationProfileService service;

    @BeforeEach
    void setUp() {
        authAccountStore = mock(AuthAccountStore.class);
        eventStore = mock(UserMusicEventStore.class);
        profileStore = new InMemoryUserPersonalizationProfileStore();
        service = new UserPersonalizationProfileService(authAccountStore, eventStore, profileStore, new EventSignalWeights());
        ReflectionTestUtils.setField(service, "eventLimit", 200);
        ReflectionTestUtils.setField(service, "topArtistLimit", 10);
        ReflectionTestUtils.setField(service, "topPlatformLimit", 5);

        when(authAccountStore.findByUserId(ADMIN_USER_ID))
            .thenReturn(Optional.of(adminAccount()));
    }

    @Test
    void shouldAggregateTopArtistsWeightedBySignalStrength() {
        when(eventStore.findRecentByUserId(TARGET_USER_ID, 200)).thenReturn(List.of(
            event("track_saved", "Queen", "tidal", null, instant("2026-05-14T00:00:00Z")),
            event("track_saved", "Queen", "tidal", null, instant("2026-05-13T00:00:00Z")),
            event("play_completed", "Queen", "tidal", null, instant("2026-05-12T00:00:00Z")),
            event("track_saved", "Radiohead", "spotify", null, instant("2026-05-11T00:00:00Z")),
            event("skipped_early", "ColdBand", "spotify", null, instant("2026-05-10T00:00:00Z"))
        ));

        UserPersonalizationProfileService.RecomputeResult result =
            service.recomputeForAdmin(ADMIN_USER_ID, TARGET_USER_ID, null);

        assertThat(result.profile().topArtists()).extracting(
            UserPersonalizationProfileStore.ArtistAffinity::artistName
        ).containsExactly("Queen", "Radiohead", "ColdBand");
        assertThat(result.profile().topArtists()).first()
            .satisfies(a -> {
                // canonical weights from EventSignalWeights: track_saved=2.0, play_completed=1.0
                assertThat(a.score()).isEqualTo(2.0d + 2.0d + 1.0d);
                assertThat(a.signalCount()).isEqualTo(3L);
            });
        assertThat(result.profile().topSourcePlatforms())
            .extracting(UserPersonalizationProfileStore.PlatformAffinity::platform)
            .containsExactly("tidal", "spotify");
        assertThat(result.profile().eventCountAtUpdate()).isEqualTo(5L);
        assertThat(result.profile().lastEventAt()).isEqualTo(instant("2026-05-14T00:00:00Z"));
        assertThat(result.eventsScanned()).isEqualTo(5);
        assertThat(result.signalCount()).isEqualTo(5L);
    }

    @Test
    void shouldUseExplicitEventWeightWhenProvided() {
        when(eventStore.findRecentByUserId(TARGET_USER_ID, 200)).thenReturn(List.of(
            eventWithWeight("custom_event", "ArtistA", "tidal", 3.0d),
            eventWithWeight("custom_event", "ArtistA", "tidal", 2.0d)
        ));

        UserPersonalizationProfileService.RecomputeResult result =
            service.recomputeForAdmin(ADMIN_USER_ID, TARGET_USER_ID, null);

        assertThat(result.profile().topArtists()).first()
            .satisfies(a -> assertThat(a.score()).isEqualTo(5.0d));
    }

    @Test
    void shouldSkipEventsWithZeroWeight() {
        when(eventStore.findRecentByUserId(TARGET_USER_ID, 200)).thenReturn(List.of(
            event("unknown_event_kind", "ArtistA", "tidal", null, instant("2026-05-14T00:00:00Z")),
            event("track_saved", "ArtistA", "tidal", null, instant("2026-05-13T00:00:00Z"))
        ));

        UserPersonalizationProfileService.RecomputeResult result =
            service.recomputeForAdmin(ADMIN_USER_ID, TARGET_USER_ID, null);

        // unknown_event_kind has no canonical weight → not counted, only the saved event contributes
        assertThat(result.signalCount()).isEqualTo(1L);
        assertThat(result.profile().topArtists()).first()
            .satisfies(a -> assertThat(a.score()).isEqualTo(2.0d)); // track_saved canonical weight
    }

    @Test
    void shouldRecomputeForAdminItselfWhenTargetMissing() {
        when(eventStore.findRecentByUserId(ADMIN_USER_ID, 200)).thenReturn(List.of(
            event("play_completed", "Solo Artist", "spotify", null, instant("2026-05-14T00:00:00Z"))
        ));

        UserPersonalizationProfileService.RecomputeResult result =
            service.recomputeForAdmin(ADMIN_USER_ID, null, null);

        assertThat(result.profile().userId()).isEqualTo(ADMIN_USER_ID);
    }

    @Test
    void shouldRejectNonAdminCallers() {
        AuthRegisteredAccount nonAdmin = new AuthRegisteredAccount(
            "user-001",
            "USER@example.com",
            "user@example.com",
            "User",
            "spotify",
            null,
            null,
            false,
            "ready",
            instant("2026-05-01T00:00:00Z"),
            instant("2026-05-01T00:00:00Z"),
            instant("2026-05-01T00:00:00Z")
        );
        when(authAccountStore.findByUserId("user-001")).thenReturn(Optional.of(nonAdmin));

        assertThatThrownBy(() -> service.recomputeForAdmin("user-001", TARGET_USER_ID, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("restricted");
    }

    @Test
    void shouldFindPersistedProfileAfterRecompute() {
        when(eventStore.findRecentByUserId(TARGET_USER_ID, 200)).thenReturn(List.of(
            event("track_saved", "Queen", "tidal", null, instant("2026-05-14T00:00:00Z"))
        ));
        service.recomputeForAdmin(ADMIN_USER_ID, TARGET_USER_ID, null);

        Optional<UserPersonalizationProfileStore.Profile> profile =
            service.findProfileForAdmin(ADMIN_USER_ID, TARGET_USER_ID);

        assertThat(profile).isPresent();
        assertThat(profile.get().topArtists()).extracting(
            UserPersonalizationProfileStore.ArtistAffinity::artistName
        ).containsExactly("Queen");
    }

    private AuthRegisteredAccount adminAccount() {
        return new AuthRegisteredAccount(
            ADMIN_USER_ID,
            ADMIN_EMAIL,
            ADMIN_EMAIL,
            "Admin",
            "spotify",
            null,
            null,
            false,
            "ready",
            instant("2026-05-01T00:00:00Z"),
            instant("2026-05-01T00:00:00Z"),
            instant("2026-05-01T00:00:00Z")
        );
    }

    private StoredEvent event(
        String eventType,
        String artist,
        String platform,
        Double weight,
        Instant occurredAt
    ) {
        return new StoredEvent(
            1L,
            TARGET_USER_ID,
            eventType,
            weight,
            "pms",
            platform,
            null,
            null,
            "track",
            "track-1",
            null,
            null,
            null,
            "Some Title",
            artist,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            occurredAt,
            occurredAt
        );
    }

    private StoredEvent eventWithWeight(String eventType, String artist, String platform, double weight) {
        return event(eventType, artist, platform, weight, instant("2026-05-14T00:00:00Z"));
    }

    private Instant instant(String iso) {
        return Instant.parse(iso);
    }
}
