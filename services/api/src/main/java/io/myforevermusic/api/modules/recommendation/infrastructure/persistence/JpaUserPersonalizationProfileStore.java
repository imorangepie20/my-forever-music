package io.myforevermusic.api.modules.recommendation.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.myforevermusic.api.modules.recommendation.application.UserPersonalizationProfileStore;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
public class JpaUserPersonalizationProfileStore implements UserPersonalizationProfileStore {

    private final UserPersonalizationProfileRepository repository;
    private final ObjectMapper objectMapper;

    public JpaUserPersonalizationProfileStore(
        UserPersonalizationProfileRepository repository,
        ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UserPersonalizationProfileStore.Profile upsert(Draft draft) {
        String artistsJson = serialize(draft.topArtists());
        String platformsJson = serialize(draft.topSourcePlatforms());
        UserPersonalizationProfileEntity entity = repository.findByUserId(draft.userId())
            .map(existing -> {
                existing.apply(
                    artistsJson,
                    platformsJson,
                    draft.eventCountAtUpdate(),
                    draft.lastEventAt(),
                    draft.recomputedAt()
                );
                return existing;
            })
            .orElseGet(() -> new UserPersonalizationProfileEntity(
                draft.userId(),
                artistsJson,
                platformsJson,
                draft.eventCountAtUpdate(),
                draft.lastEventAt(),
                draft.recomputedAt()
            ));
        UserPersonalizationProfileEntity saved = repository.save(entity);
        return toProfile(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserPersonalizationProfileStore.Profile> findByUserId(String userId) {
        return repository.findByUserId(userId).map(this::toProfile);
    }

    private UserPersonalizationProfileStore.Profile toProfile(UserPersonalizationProfileEntity entity) {
        return new UserPersonalizationProfileStore.Profile(
            entity.getId(),
            entity.getUserId(),
            deserialize(entity.getTopArtistsJson(), new TypeReference<List<ArtistAffinity>>() {}),
            deserialize(entity.getTopSourcePlatformsJson(), new TypeReference<List<PlatformAffinity>>() {}),
            entity.getEventCountAtUpdate(),
            entity.getLastEventAt(),
            entity.getRecomputedAt()
        );
    }

    private <T> String serialize(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize personalization profile field.", ex);
        }
    }

    private <T> List<T> deserialize(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
