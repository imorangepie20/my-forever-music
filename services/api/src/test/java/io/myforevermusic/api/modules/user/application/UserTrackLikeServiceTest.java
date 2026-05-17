package io.myforevermusic.api.modules.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.user.infrastructure.persistence.UserTrackLikeEntity;
import io.myforevermusic.api.modules.user.infrastructure.persistence.UserTrackLikeRepository;
import io.myforevermusic.api.modules.user.presentation.UserTrackLikeListResponse;
import io.myforevermusic.api.modules.user.presentation.UserTrackLikeRequest;
import io.myforevermusic.api.modules.user.presentation.UserTrackLikeResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class UserTrackLikeServiceTest {

    private final UserTrackLikeRepository repository = mock(UserTrackLikeRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-17T12:00:00Z"), ZoneOffset.UTC);
    private final UserTrackLikeService service = new UserTrackLikeService(repository, clock);

    @Test
    void togglingNewTrackSavesAndReturnsLikedTrue() {
        when(repository.findByUserIdAndSourcePlatformAndExternalTrackId("user-1", "spotify", "track-1"))
            .thenReturn(Optional.empty());
        when(repository.save(any(UserTrackLikeEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserTrackLikeResponse response = service.toggle(new UserTrackLikeRequest(
            "user-1",
            "spotify",
            "track-1",
            "Title",
            "Artist",
            "Album",
            "https://image",
            "spotify-id",
            "https://platform"
        ));

        assertThat(response.liked()).isTrue();
        assertThat(response.externalTrackId()).isEqualTo("track-1");
        assertThat(response.likedAt()).isEqualTo(Instant.parse("2026-05-17T12:00:00Z"));
        verify(repository).save(any(UserTrackLikeEntity.class));
    }

    @Test
    void togglingExistingTrackDeletesAndReturnsLikedFalse() {
        UserTrackLikeEntity existing = new UserTrackLikeEntity(
            "user-1",
            "spotify",
            "track-1",
            "Title",
            "Artist",
            "Album",
            "https://image",
            "spotify-id",
            "https://platform",
            Instant.parse("2026-05-17T10:00:00Z")
        );
        when(repository.findByUserIdAndSourcePlatformAndExternalTrackId("user-1", "spotify", "track-1"))
            .thenReturn(Optional.of(existing));

        UserTrackLikeResponse response = service.toggle(new UserTrackLikeRequest(
            "user-1", "spotify", "track-1", null, null, null, null, null, null
        ));

        assertThat(response.liked()).isFalse();
        assertThat(response.likedAt()).isNull();
        verify(repository).delete(existing);
        verify(repository, times(0)).save(any());
    }

    @Test
    void getStateReturnsLikedFalseWhenAbsent() {
        when(repository.findByUserIdAndSourcePlatformAndExternalTrackId("user-1", "spotify", "track-1"))
            .thenReturn(Optional.empty());

        UserTrackLikeResponse response = service.getState("user-1", "spotify", "track-1");

        assertThat(response.liked()).isFalse();
        assertThat(response.likedAt()).isNull();
    }

    @Test
    void getStateReturnsLikedTrueWhenPresent() {
        UserTrackLikeEntity entity = new UserTrackLikeEntity(
            "user-1", "spotify", "track-1",
            null, null, null, null, null, null,
            Instant.parse("2026-05-17T10:00:00Z")
        );
        when(repository.findByUserIdAndSourcePlatformAndExternalTrackId("user-1", "spotify", "track-1"))
            .thenReturn(Optional.of(entity));

        UserTrackLikeResponse response = service.getState("user-1", "spotify", "track-1");

        assertThat(response.liked()).isTrue();
        assertThat(response.likedAt()).isEqualTo(Instant.parse("2026-05-17T10:00:00Z"));
    }

    @Test
    void listReturnsLikedTracksWithTotal() {
        UserTrackLikeEntity entity = new UserTrackLikeEntity(
            "user-1", "spotify", "track-1",
            "Title 1", "Artist 1", "Album", "https://image", "sp-id", "https://platform",
            Instant.parse("2026-05-17T10:00:00Z")
        );
        when(repository.findByUserIdOrderByLikedAtDesc(eq("user-1"), any(Pageable.class)))
            .thenReturn(List.of(entity));
        when(repository.countByUserId("user-1")).thenReturn(7L);

        UserTrackLikeListResponse response = service.list("user-1", 50);

        assertThat(response.totalCount()).isEqualTo(7L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).externalTrackId()).isEqualTo("track-1");
        assertThat(response.items().get(0).title()).isEqualTo("Title 1");
    }

    @Test
    void rejectsBlankIdentifiers() {
        assertThatThrownBy(() -> service.toggle(new UserTrackLikeRequest(
            "", "spotify", "track-1", null, null, null, null, null, null
        ))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getState("user-1", "", "track-1"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list("", 10))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
