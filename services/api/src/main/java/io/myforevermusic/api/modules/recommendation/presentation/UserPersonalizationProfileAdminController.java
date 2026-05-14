package io.myforevermusic.api.modules.recommendation.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileService;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.ArtistAffinity;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.PlatformAffinity;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore.Profile;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/recommendations/admin/personalization-profile")
public class UserPersonalizationProfileAdminController {

    private final UserPersonalizationProfileService service;

    public UserPersonalizationProfileAdminController(UserPersonalizationProfileService service) {
        this.service = service;
    }

    @Operation(summary = "Fetch the persisted personalization profile for a user (admin only)")
    @GetMapping
    public PersonalizationProfileResponse getProfile(
        @RequestParam("user_id") String userId,
        @RequestParam(value = "target_user_id", required = false) String targetUserId
    ) {
        Optional<Profile> profile = service.findProfileForAdmin(userId, targetUserId);
        return profile
            .map(value -> PersonalizationProfileResponse.found(value, Instant.now()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "personalization profile has not been computed for this user yet."
            ));
    }

    @Operation(summary = "Recompute the personalization profile for a user from recent events (admin only)")
    @PostMapping("/recompute")
    public PersonalizationProfileRecomputeResponse recompute(
        @RequestParam("user_id") String userId,
        @RequestParam(value = "target_user_id", required = false) String targetUserId,
        @RequestParam(value = "event_limit", required = false) Integer eventLimit
    ) {
        UserPersonalizationProfileService.RecomputeResult result = service.recomputeForAdmin(
            userId,
            targetUserId,
            eventLimit
        );
        return PersonalizationProfileRecomputeResponse.from(result, Instant.now());
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PersonalizationProfileResponse(
        String service,
        String status,
        Instant generatedAt,
        ProfileItem profile
    ) {
        static PersonalizationProfileResponse found(Profile profile, Instant generatedAt) {
            return new PersonalizationProfileResponse("api", "ok", generatedAt, ProfileItem.from(profile));
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PersonalizationProfileRecomputeResponse(
        String service,
        String status,
        Instant generatedAt,
        int eventsScanned,
        long signalCount,
        int eventLimit,
        ProfileItem profile
    ) {
        static PersonalizationProfileRecomputeResponse from(
            UserPersonalizationProfileService.RecomputeResult result,
            Instant generatedAt
        ) {
            return new PersonalizationProfileRecomputeResponse(
                "api",
                "ok",
                generatedAt,
                result.eventsScanned(),
                result.signalCount(),
                result.eventLimit(),
                ProfileItem.from(result.profile())
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProfileItem(
        Long profileId,
        String userId,
        List<ArtistAffinityItem> topArtists,
        List<PlatformAffinityItem> topSourcePlatforms,
        long eventCountAtUpdate,
        Instant lastEventAt,
        Instant recomputedAt
    ) {
        static ProfileItem from(Profile profile) {
            return new ProfileItem(
                profile.profileId(),
                profile.userId(),
                profile.topArtists() == null
                    ? List.of()
                    : profile.topArtists().stream().map(ArtistAffinityItem::from).toList(),
                profile.topSourcePlatforms() == null
                    ? List.of()
                    : profile.topSourcePlatforms().stream().map(PlatformAffinityItem::from).toList(),
                profile.eventCountAtUpdate(),
                profile.lastEventAt(),
                profile.recomputedAt()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ArtistAffinityItem(String artistName, double score, long signalCount) {
        static ArtistAffinityItem from(ArtistAffinity affinity) {
            return new ArtistAffinityItem(affinity.artistName(), affinity.score(), affinity.signalCount());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PlatformAffinityItem(String platform, double score, long signalCount) {
        static PlatformAffinityItem from(PlatformAffinity affinity) {
            return new PlatformAffinityItem(affinity.platform(), affinity.score(), affinity.signalCount());
        }
    }
}
