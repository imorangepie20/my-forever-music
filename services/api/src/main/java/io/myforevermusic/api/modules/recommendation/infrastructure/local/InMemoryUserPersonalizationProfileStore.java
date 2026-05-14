package io.myforevermusic.api.modules.recommendation.infrastructure.local;

import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryUserPersonalizationProfileStore implements UserPersonalizationProfileStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final ConcurrentHashMap<String, Profile> profilesByUserId = new ConcurrentHashMap<>();

    @Override
    public Profile upsert(Draft draft) {
        return profilesByUserId.compute(draft.userId(), (key, existing) -> {
            long id = existing == null ? sequence.getAndIncrement() : existing.profileId();
            return new Profile(
                id,
                draft.userId(),
                draft.topArtists() == null ? java.util.List.of() : draft.topArtists(),
                draft.topSourcePlatforms() == null ? java.util.List.of() : draft.topSourcePlatforms(),
                draft.eventCountAtUpdate(),
                draft.lastEventAt(),
                draft.recomputedAt()
            );
        });
    }

    @Override
    public Optional<Profile> findByUserId(String userId) {
        return Optional.ofNullable(profilesByUserId.get(userId));
    }
}
