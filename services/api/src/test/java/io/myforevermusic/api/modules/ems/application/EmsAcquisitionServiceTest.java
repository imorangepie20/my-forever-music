package io.myforevermusic.api.modules.ems.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.application.EmsAcquisitionSignalModel.EmsAcquisitionSignal;
import io.myforevermusic.api.modules.ems.application.EmsAcquisitionSignalModel.EmsAcquisitionSignalModelRequest;
import io.myforevermusic.api.modules.ems.application.EmsAcquisitionSignalModel.EmsAcquisitionSignalModelResponse;
import io.myforevermusic.api.modules.ems.application.EmsCollectionService.EmsCollectionSearchPreviewResult;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionRunEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionRunRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionSeedEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionSeedRepository;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionSignalEntity;
import io.myforevermusic.api.modules.ems.infrastructure.persistence.EmsAcquisitionSignalRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmsAcquisitionServiceTest {

    @Mock
    private EmsEditorialSourceClient sourceClient;

    @Mock
    private EmsAcquisitionSignalModel signalModel;

    @Mock
    private EmsCollectionService collectionService;

    @Mock
    private EmsAcquisitionRunRepository runRepository;

    @Mock
    private EmsAcquisitionSignalRepository signalRepository;

    @Mock
    private EmsAcquisitionSeedRepository seedRepository;

    @Test
    void shouldExtractSignalsAndQueueProviderPoolRuns() {
        EmsAcquisitionProperties properties = properties();
        EmsEditorialSource source = new EmsEditorialSource("Pitchfork", "rss", "https://pitchfork.com/feed/", 1.0d);
        EmsEditorialArticle article = new EmsEditorialArticle(
            "Pitchfork",
            "https://pitchfork.com/feed/",
            "https://example.test/a",
            "Best New Tracks",
            "A roundup of new tracks.",
            Instant.parse("2026-05-14T00:00:00Z")
        );
        List<EmsAcquisitionSignalEntity> savedSignals = new ArrayList<>();
        List<EmsAcquisitionSeedEntity> savedSeeds = new ArrayList<>();
        AtomicLong ids = new AtomicLong(1);

        when(runRepository.save(any(EmsAcquisitionRunEntity.class))).thenAnswer(invocation -> {
            EmsAcquisitionRunEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", ids.getAndIncrement());
            }
            return entity;
        });
        when(signalRepository.save(any(EmsAcquisitionSignalEntity.class))).thenAnswer(invocation -> {
            EmsAcquisitionSignalEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", ids.getAndIncrement());
            savedSignals.add(entity);
            return entity;
        });
        when(seedRepository.save(any(EmsAcquisitionSeedEntity.class))).thenAnswer(invocation -> {
            EmsAcquisitionSeedEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", ids.getAndIncrement());
            }
            if (!savedSeeds.contains(entity)) {
                savedSeeds.add(entity);
            }
            return entity;
        });
        when(signalRepository.findTop50ByRunIdOrderByIdAsc(1L)).thenReturn(savedSignals);
        when(seedRepository.findTop100ByRunIdOrderByIdAsc(1L)).thenReturn(savedSeeds);
        when(sourceClient.fetch(source, 10)).thenReturn(List.of(article));
        when(signalModel.extractSignals(any(EmsAcquisitionSignalModelRequest.class)))
            .thenReturn(new EmsAcquisitionSignalModelResponse(
                "ai-001",
                Instant.parse("2026-05-14T00:00:01Z"),
                "test-model",
                List.of(new EmsAcquisitionSignal(
                    "https://example.test/a",
                    "Best New Tracks",
                    "playlist_query",
                    "best new tracks",
                    0.84d,
                    "Editorial roundup"
                ))
            ));
        when(collectionService.queueAcquisitionSearchPool("user-001", "spotify", "best new tracks", 5))
            .thenReturn(new EmsCollectionSearchPreviewResult(
                "spotify",
                "best new tracks",
                44L,
                List.of(),
                List.of(),
                5,
                4,
                Instant.parse("2026-05-14T00:00:02Z")
            ));

        EmsAcquisitionService service = new EmsAcquisitionService(
            properties,
            sourceClient,
            signalModel,
            collectionService,
            runRepository,
            signalRepository,
            seedRepository
        );

        EmsAcquisitionService.EmsAcquisitionRunDetailSnapshot result = service.runNow(
            new EmsAcquisitionService.EmsAcquisitionRunCommand(
                "user-001",
                List.of("spotify"),
                List.of(sourceProperty("Pitchfork", "rss", "https://pitchfork.com/feed/")),
                10,
                5,
                5
            )
        );

        assertThat(result.run().status()).isEqualTo("completed");
        assertThat(result.run().signalCount()).isEqualTo(1);
        assertThat(result.run().poolRunCount()).isEqualTo(1);
        assertThat(result.signals()).singleElement().satisfies(signal ->
            assertThat(signal.query()).isEqualTo("best new tracks")
        );
        assertThat(result.seeds()).singleElement().satisfies(seed -> {
            assertThat(seed.platformId()).isEqualTo("spotify");
            assertThat(seed.poolRunId()).isEqualTo(44L);
        });
        verify(collectionService).queueAcquisitionSearchPool("user-001", "spotify", "best new tracks", 5);
    }

    private EmsAcquisitionProperties properties() {
        EmsAcquisitionProperties properties = new EmsAcquisitionProperties();
        properties.setUserId("user-001");
        properties.setPlatforms(List.of("spotify"));
        properties.setMaxArticlesPerSource(10);
        properties.setMaxSignalsPerRun(5);
        properties.setPerSeedLimit(5);
        return properties;
    }

    private EmsAcquisitionProperties.Source sourceProperty(String name, String type, String url) {
        EmsAcquisitionProperties.Source source = new EmsAcquisitionProperties.Source();
        source.setName(name);
        source.setType(type);
        source.setUrl(url);
        source.setWeight(1.0d);
        return source;
    }
}
