# TIDAL Playlist URL GMS Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a normal logged-in user paste a real TIDAL playlist URL, persist that playlist into EMS, and surface it through the existing GMS playlist candidate flow.

**Architecture:** Add the smallest real-data path: TIDAL client fetches playlist metadata by id, EMS collects one concrete TIDAL playlist into existing EMS tables, GMS exposes an import endpoint and can include the just-imported playlist id in the next preview response. The web page adds a compact URL form and reuses the existing candidate UI.

**Tech Stack:** Spring Boot 3.5, Java 21, JUnit 5, Mockito, React, TypeScript, Vite.

---

## File Structure

- Modify `services/api/src/main/java/io/myforevermusic/api/modules/platform/infrastructure/tidal/TidalWebApiClient.java`
  - Add a public `getPlaylist(PlatformAccountCredential credential, String playlistId)` method that calls real TIDAL API metadata.
- Modify `services/api/src/test/java/io/myforevermusic/api/modules/platform/infrastructure/tidal/TidalWebApiClientTest.java`
  - Cover playlist metadata parsing by id.
- Modify `services/api/src/main/java/io/myforevermusic/api/modules/ems/application/EmsCollectionService.java`
  - Add `collectTidalPlaylistFromUrlImport(userId, playlistId)` and a result record.
- Modify `services/api/src/test/java/io/myforevermusic/api/modules/ems/application/EmsCollectionServiceTest.java`
  - Cover successful URL import collection and empty playlist failure.
- Create `services/api/src/main/java/io/myforevermusic/api/modules/gms/application/GmsTidalPlaylistUrlImportService.java`
  - Parse TIDAL URL, delegate EMS collection, and return import result.
- Modify `services/api/src/main/java/io/myforevermusic/api/modules/gms/application/GmsPlaylistPreviewService.java`
  - Add optional explicit playlist inclusion for the just-imported EMS playlist.
- Modify `services/api/src/main/java/io/myforevermusic/api/modules/gms/presentation/GmsPlaylistPreviewController.java`
  - Add the import endpoint and `include_playlist_id` preview parameter.
- Modify `services/api/src/test/java/io/myforevermusic/api/modules/gms/application/GmsPlaylistPreviewServiceTest.java`
  - Cover explicit playlist inclusion despite preferred-platform filtering.
- Create `services/api/src/test/java/io/myforevermusic/api/modules/gms/application/GmsTidalPlaylistUrlImportServiceTest.java`
  - Cover URL parsing and invalid URL rejection.
- Modify `apps/web/src/types/api.ts`
  - Add import request/response types.
- Modify `apps/web/src/services/api.ts`
  - Add `importTidalPlaylistUrlToGms` and optional `includePlaylistId` for preview fetch.
- Modify `apps/web/src/pages/GmsPlaylistsPage.tsx`
  - Add URL form, submit state, success message, and refresh with included playlist id.

---

## Task 1: TIDAL Playlist Metadata Lookup

**Files:**
- Modify: `services/api/src/main/java/io/myforevermusic/api/modules/platform/infrastructure/tidal/TidalWebApiClient.java`
- Test: `services/api/src/test/java/io/myforevermusic/api/modules/platform/infrastructure/tidal/TidalWebApiClientTest.java`

- [ ] **Step 1: Write the failing metadata test**

Append this test to `TidalWebApiClientTest`:

```java
@Test
void shouldFetchPlaylistMetadataById() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v2/playlists/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33", exchange -> {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("countryCode=KR")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        byte[] response = """
            {
              "data": {
                "id": "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33",
                "type": "playlists",
                "attributes": {
                  "name": "Night Drive Imports",
                  "description": "Public TIDAL playlist",
                  "numberOfItems": 24,
                  "imageId": "ab12cd34-ef56-7890-ab12-cd34ef567890",
                  "url": "https://tidal.com/playlist/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
                }
              }
            }
            """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/vnd.api+json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    });
    server.start();

    try {
        PlatformOAuthProperties properties = new PlatformOAuthProperties();
        properties.getTidal().setCountryCode("KR");
        properties.getTidal().setApiBaseUri("http://127.0.0.1:%d/v2".formatted(server.getAddress().getPort()));
        TidalWebApiClient client = new TidalWebApiClient(
            properties,
            new ObjectMapper(),
            HttpClient.newHttpClient(),
            properties.getTidal().getApiBaseUri()
        );

        TidalWebApiClient.TidalPlaylistSummary playlist = client.getPlaylist(
            tidalCredential(),
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
        );

        assertThat(playlist.playlistId()).isEqualTo("0a3d87d2-27dc-4edc-84b6-9f1eaa567f33");
        assertThat(playlist.name()).isEqualTo("Night Drive Imports");
        assertThat(playlist.description()).isEqualTo("Public TIDAL playlist");
        assertThat(playlist.trackCount()).isEqualTo(24);
        assertThat(playlist.externalUrl()).isEqualTo("https://tidal.com/playlist/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33");
    } finally {
        server.stop(0);
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
./gradlew test --tests io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClientTest.shouldFetchPlaylistMetadataById
```

Expected: compile failure because `getPlaylist(...)` does not exist.

- [ ] **Step 3: Implement `getPlaylist`**

Add this method near `getUserPlaylists` in `TidalWebApiClient.java`:

```java
public TidalPlaylistSummary getPlaylist(
    PlatformAccountCredential credential,
    String playlistId
) {
    String countryCode = countryCodeForCredential(credential);
    try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("%s/playlists/%s?countryCode=%s".formatted(
                apiBaseUri,
                URLEncoder.encode(playlistId, StandardCharsets.UTF_8),
                countryCode
            )))
            .header("Accept", ACCEPT_HEADER)
            .header("Authorization", "Bearer %s".formatted(credential.accessToken()))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException("TIDAL playlist metadata request failed: %s".formatted(response.statusCode()));
        }

        JsonApiRoot jsonApi = objectMapper.readValue(response.body(), JsonApiRoot.class);
        return Optional.ofNullable(jsonApi.data())
            .filter(data -> "playlists".equals(data.type()))
            .map(this::toPlaylistSummary)
            .orElseThrow(() -> new IllegalArgumentException("TIDAL playlist metadata response missing playlist data"));
    } catch (IOException exception) {
        throw new IllegalStateException("TIDAL playlist metadata response could not be parsed.", exception);
    } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("TIDAL playlist metadata request was interrupted.", exception);
    }
}
```

- [ ] **Step 4: Verify TIDAL client tests**

Run:

```bash
./gradlew test --tests io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClientTest
```

Expected: all `TidalWebApiClientTest` tests pass.

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/io/myforevermusic/api/modules/platform/infrastructure/tidal/TidalWebApiClient.java services/api/src/test/java/io/myforevermusic/api/modules/platform/infrastructure/tidal/TidalWebApiClientTest.java
git commit -m "feat: fetch tidal playlist metadata by id"
```

---

## Task 2: EMS Single TIDAL Playlist Collection

**Files:**
- Modify: `services/api/src/main/java/io/myforevermusic/api/modules/ems/application/EmsCollectionService.java`
- Test: `services/api/src/test/java/io/myforevermusic/api/modules/ems/application/EmsCollectionServiceTest.java`

- [ ] **Step 1: Write failing EMS collection tests**

Add imports if missing:

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

Append these tests to `EmsCollectionServiceTest`:

```java
@Test
void shouldCollectTidalPlaylistUrlImportIntoEms() {
    PlatformAccountCredential credential = credential("tidal");
    io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistSummary playlist =
        new io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistSummary(
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33",
            "Night Drive Imports",
            "Public TIDAL playlist",
            1,
            null,
            null,
            "https://tidal.com/playlist/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33",
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
        );
    io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistTrack track =
        new io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistTrack(
            "tidal-track-001",
            "Imported Track",
            "Imported Artist",
            "Imported Album",
            null,
            "https://tidal.com/browse/track/tidal-track-001",
            "tidal:track:tidal-track-001",
            null,
            "USRC17607839",
            180000
        );
    EmsCollectedPlaylistEntity savedPlaylist = collectedPlaylist(
        "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33",
        "tidal",
        "Night Drive Imports",
        1
    );
    ReflectionTestUtils.setField(savedPlaylist, "id", 70L);
    EmsCollectedTrackEntity savedTrack = collectedTrack(
        "tidal-track-001",
        "tidal",
        "Imported Track",
        "Imported Artist",
        "USRC17607839"
    );
    ReflectionTestUtils.setField(savedTrack, "id", 80L);

    when(platformCredentialService.findUsableCredential("user-001", "tidal"))
        .thenReturn(Optional.of(credential));
    when(tidalWebApiClient.getPlaylist(credential, "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"))
        .thenReturn(playlist);
    when(tidalWebApiClient.getPlaylistTracks(credential, "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"))
        .thenReturn(List.of(track));
    when(reccoBeatsAudioFeaturesClient.getAudioFeaturesForExternalTracksByIsrc(any()))
        .thenReturn(Map.of());
    when(playlistRepository.findBySourcePlatformAndExternalPlaylistId("tidal", playlist.playlistId()))
        .thenReturn(Optional.empty());
    when(playlistRepository.save(any(EmsCollectedPlaylistEntity.class))).thenReturn(savedPlaylist);
    when(trackRepository.findBySourcePlatformAndExternalTrackId("tidal", "tidal-track-001"))
        .thenReturn(Optional.empty());
    when(trackRepository.save(any(EmsCollectedTrackEntity.class))).thenReturn(savedTrack);

    EmsCollectionService.EmsTidalPlaylistUrlImportCollection result =
        service().collectTidalPlaylistFromUrlImport("user-001", playlist.playlistId());

    assertThat(result.emsPlaylistId()).isEqualTo(70L);
    assertThat(result.externalPlaylistId()).isEqualTo(playlist.playlistId());
    assertThat(result.title()).isEqualTo("Night Drive Imports");
    assertThat(result.trackCount()).isEqualTo(1);
    assertThat(result.collectionSource()).isEqualTo("user_tidal_url_import");
    verify(playlistTrackRepository).upsertPlaylistTrackLink(70L, 80L, 0);
}

@Test
void shouldRejectTidalPlaylistUrlImportWhenPlaylistHasNoTracks() {
    PlatformAccountCredential credential = credential("tidal");
    io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistSummary playlist =
        new io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClient.TidalPlaylistSummary(
            "empty-playlist",
            "Empty Playlist",
            "",
            0,
            null,
            null,
            "https://tidal.com/playlist/empty-playlist",
            "empty-playlist"
        );

    when(platformCredentialService.findUsableCredential("user-001", "tidal"))
        .thenReturn(Optional.of(credential));
    when(tidalWebApiClient.getPlaylist(credential, "empty-playlist")).thenReturn(playlist);
    when(tidalWebApiClient.getPlaylistTracks(credential, "empty-playlist")).thenReturn(List.of());

    assertThatThrownBy(() -> service().collectTidalPlaylistFromUrlImport("user-001", "empty-playlist"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not contain importable tracks");
}
```

- [ ] **Step 2: Run the failing EMS tests**

Run:

```bash
./gradlew test --tests io.myforevermusic.api.modules.ems.application.EmsCollectionServiceTest
```

Expected: compile failure because `collectTidalPlaylistFromUrlImport(...)` and result record do not exist.

- [ ] **Step 3: Implement EMS import collection**

In `EmsCollectionService.java`, add a constant near `SEARCH_POOL_SOURCE`:

```java
public static final String USER_TIDAL_URL_IMPORT_SOURCE = "user_tidal_url_import";
```

Add this transactional method:

```java
@Transactional
public EmsTidalPlaylistUrlImportCollection collectTidalPlaylistFromUrlImport(String userId, String playlistId) {
    Instant collectedAt = Instant.now();
    PlatformAccountCredential credential = platformCredentialService
        .findUsableCredential(userId, "tidal")
        .orElseThrow(() -> new IllegalArgumentException("Connect TIDAL before importing a TIDAL playlist URL into GMS."));

    TidalPlaylistSummary playlist = tidalWebApiClient.getPlaylist(credential, playlistId);
    List<TidalPlaylistTrack> tracks = tidalWebApiClient.getPlaylistTracks(credential, playlistId);
    if (tracks.isEmpty()) {
        throw new IllegalArgumentException("TIDAL playlist does not contain importable tracks: %s".formatted(playlistId));
    }

    EmsCollectedPlaylistEntity playlistEntity = upsertPlaylistFromTidal(
        playlist,
        USER_TIDAL_URL_IMPORT_SOURCE,
        playlistId,
        collectedAt
    );
    Map<String, ReccoBeatsAudioFeaturesSnapshot> audioFeaturesByTrackId = resolveTidalAudioFeatures(tracks);
    Instant resolvedAt = Instant.now();
    for (int i = 0; i < tracks.size(); i++) {
        TidalPlaylistTrack track = tracks.get(i);
        EmsCollectedTrackEntity trackEntity = upsertTrackFromTidal(
            track,
            USER_TIDAL_URL_IMPORT_SOURCE,
            collectedAt,
            resolveTidalTrackAudioFeatures(track, audioFeaturesByTrackId.get(track.tidalTrackId()), resolvedAt)
        );
        linkPlaylistTrack(playlistEntity, trackEntity, i);
    }

    return new EmsTidalPlaylistUrlImportCollection(
        playlistEntity.getId(),
        playlist.playlistId(),
        "tidal",
        playlistEntity.getTitle(),
        tracks.size(),
        USER_TIDAL_URL_IMPORT_SOURCE,
        collectedAt
    );
}
```

Add the record near the other public result records:

```java
public record EmsTidalPlaylistUrlImportCollection(
    Long emsPlaylistId,
    String externalPlaylistId,
    String sourcePlatform,
    String title,
    int trackCount,
    String collectionSource,
    Instant collectedAt
) {}
```

- [ ] **Step 4: Verify EMS collection tests**

Run:

```bash
./gradlew test --tests io.myforevermusic.api.modules.ems.application.EmsCollectionServiceTest
```

Expected: all `EmsCollectionServiceTest` tests pass.

- [ ] **Step 5: Commit**

```bash
git add services/api/src/main/java/io/myforevermusic/api/modules/ems/application/EmsCollectionService.java services/api/src/test/java/io/myforevermusic/api/modules/ems/application/EmsCollectionServiceTest.java
git commit -m "feat: collect tidal url playlists into ems"
```

---

## Task 3: GMS Import Endpoint and Explicit Preview Inclusion

**Files:**
- Create: `services/api/src/main/java/io/myforevermusic/api/modules/gms/application/GmsTidalPlaylistUrlImportService.java`
- Modify: `services/api/src/main/java/io/myforevermusic/api/modules/gms/application/GmsPlaylistPreviewService.java`
- Modify: `services/api/src/main/java/io/myforevermusic/api/modules/gms/presentation/GmsPlaylistPreviewController.java`
- Test: `services/api/src/test/java/io/myforevermusic/api/modules/gms/application/GmsPlaylistPreviewServiceTest.java`
- Test: `services/api/src/test/java/io/myforevermusic/api/modules/gms/application/GmsTidalPlaylistUrlImportServiceTest.java`

- [ ] **Step 1: Write URL parser service tests**

Create `GmsTidalPlaylistUrlImportServiceTest.java`:

```java
package io.myforevermusic.api.modules.gms.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.application.EmsCollectionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GmsTidalPlaylistUrlImportServiceTest {

    @Test
    void shouldImportValidTidalPlaylistUrl() {
        EmsCollectionService emsCollectionService = mock(EmsCollectionService.class);
        when(emsCollectionService.collectTidalPlaylistFromUrlImport(
            "user-001",
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
        )).thenReturn(new EmsCollectionService.EmsTidalPlaylistUrlImportCollection(
            70L,
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33",
            "tidal",
            "Night Drive Imports",
            12,
            "user_tidal_url_import",
            Instant.parse("2026-05-19T00:00:00Z")
        ));

        GmsTidalPlaylistUrlImportService.ImportResult result =
            new GmsTidalPlaylistUrlImportService(emsCollectionService).importUrl(
                "user-001",
                "https://tidal.com/playlist/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
            );

        assertThat(result.emsPlaylistId()).isEqualTo(70L);
        assertThat(result.externalPlaylistId()).isEqualTo("0a3d87d2-27dc-4edc-84b6-9f1eaa567f33");
        assertThat(result.trackCount()).isEqualTo(12);
    }

    @Test
    void shouldRejectNonTidalUrl() {
        GmsTidalPlaylistUrlImportService service =
            new GmsTidalPlaylistUrlImportService(mock(EmsCollectionService.class));

        assertThatThrownBy(() -> service.importUrl("user-001", "https://example.com/playlist/abc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("TIDAL playlist URL");
    }
}
```

- [ ] **Step 2: Write preview inclusion test**

Append this test to `GmsPlaylistPreviewServiceTest`:

```java
@Test
void shouldIncludeExplicitImportedPlaylistEvenWhenPreferredPlatformDiffers() {
    AuthAccountStore authAccountStore = mock(AuthAccountStore.class);
    PmsUserLibraryStore pmsUserLibraryStore = mock(PmsUserLibraryStore.class);
    EmsCollectedPlaylistRepository playlistRepository = mock(EmsCollectedPlaylistRepository.class);
    EmsCollectedPlaylistTrackRepository playlistTrackRepository = mock(EmsCollectedPlaylistTrackRepository.class);
    PmsPersonalPlaylistStore personalPlaylistStore = new InMemoryPmsPersonalPlaylistStore();
    InMemoryUserMusicEventStore userMusicEventStore = new InMemoryUserMusicEventStore();
    UserMusicEventService userMusicEventService = new UserMusicEventService(userMusicEventStore, new EventSignalWeights());
    PlaylistQualityEvaluator playlistQualityEvaluator = mock(PlaylistQualityEvaluator.class);

    EmsCollectedPlaylistEntity spotifyPlaylist = playlist(1L);
    EmsCollectedPlaylistEntity tidalPlaylist = new EmsCollectedPlaylistEntity(
        "tidal-playlist-001",
        "Imported TIDAL Playlist",
        "tidal",
        "TIDAL",
        "description",
        null,
        null,
        null,
        1,
        "user_tidal_url_import",
        null,
        Instant.parse("2026-05-19T00:00:00Z")
    );
    ReflectionTestUtils.setField(tidalPlaylist, "id", 2L);
    EmsCollectedTrackEntity tidalTrack = new EmsCollectedTrackEntity(
        "tidal-track-001",
        "Imported Track",
        "Artist",
        "tidal",
        "USRC17607839",
        "Album",
        null,
        null,
        "tidal:track:tidal-track-001",
        null,
        180000,
        "user_tidal_url_import",
        Instant.parse("2026-05-19T00:00:00Z"),
        null
    );
    ReflectionTestUtils.setField(tidalTrack, "id", 20L);

    when(authAccountStore.findByUserId("user-001")).thenReturn(Optional.of(account("spotify")));
    when(pmsUserLibraryStore.findPlaylists("user-001")).thenReturn(List.of(pmsLibraryPlaylist()));
    when(playlistRepository.findRecentWithTracksBySourcePlatforms(List.of("spotify"), PageRequest.of(0, 36)))
        .thenReturn(List.of(spotifyPlaylist));
    when(playlistRepository.findById(2L)).thenReturn(Optional.of(tidalPlaylist));
    when(playlistTrackRepository.findByPlaylistIdOrderBySortOrderAsc(2L)).thenReturn(List.of(
        new EmsCollectedPlaylistTrackEntity(tidalPlaylist, tidalTrack, 1)
    ));

    GmsPlaylistPreviewService service = new GmsPlaylistPreviewService(
        authAccountStore,
        pmsUserLibraryStore,
        playlistRepository,
        playlistTrackRepository,
        personalPlaylistStore,
        userMusicEventService,
        userMusicEventStore,
        playlistQualityEvaluator
    );

    GmsPlaylistPreviewService.GmsPlaylistPreviewResult result = service.preview("user-001", 12, 2L);

    assertThat(result.candidates()).extracting(GmsPlaylistPreviewService.GmsPlaylistPreviewCandidate::playlistId)
        .contains(2L);
}
```

Add this helper to `GmsPlaylistPreviewServiceTest` before `pmsLibraryPlaylist()`:

```java
private static AuthRegisteredAccount account(String preferredPlatformId) {
    return new AuthRegisteredAccount(
        "user-001",
        "user@example.com",
        "User",
        preferredPlatformId,
        "platform-selection",
        Instant.parse("2026-05-15T00:00:00Z"),
        Instant.parse("2026-05-15T00:00:00Z")
    );
}
```

- [ ] **Step 3: Run failing GMS tests**

Run:

```bash
./gradlew test --tests io.myforevermusic.api.modules.gms.application.GmsTidalPlaylistUrlImportServiceTest --tests io.myforevermusic.api.modules.gms.application.GmsPlaylistPreviewServiceTest
```

Expected: compile failure because the new service and `preview(userId, limit, includePlaylistId)` overload do not exist.

- [ ] **Step 4: Create GMS import service**

Create `GmsTidalPlaylistUrlImportService.java`:

```java
package io.myforevermusic.api.modules.gms.application;

import io.myforevermusic.api.modules.ems.application.EmsCollectionService;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class GmsTidalPlaylistUrlImportService {

    private static final Pattern TIDAL_PLAYLIST_URL = Pattern.compile(
        "^https?://(?:www\\.)?tidal\\.com/(?:browse/)?playlist/([A-Za-z0-9][A-Za-z0-9_-]{2,159})(?:[/?#].*)?$"
    );

    private final EmsCollectionService emsCollectionService;

    public GmsTidalPlaylistUrlImportService(EmsCollectionService emsCollectionService) {
        this.emsCollectionService = emsCollectionService;
    }

    public ImportResult importUrl(String userId, String playlistUrl) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id is required.");
        }
        String playlistId = parsePlaylistId(playlistUrl);
        EmsCollectionService.EmsTidalPlaylistUrlImportCollection collected =
            emsCollectionService.collectTidalPlaylistFromUrlImport(userId, playlistId);
        return new ImportResult(
            userId,
            collected.emsPlaylistId(),
            collected.externalPlaylistId(),
            collected.sourcePlatform(),
            collected.title(),
            collected.trackCount(),
            collected.collectionSource(),
            collected.collectedAt()
        );
    }

    private String parsePlaylistId(String playlistUrl) {
        if (playlistUrl == null || playlistUrl.isBlank()) {
            throw new IllegalArgumentException("TIDAL playlist URL is required.");
        }
        Matcher matcher = TIDAL_PLAYLIST_URL.matcher(playlistUrl.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Enter a valid TIDAL playlist URL.");
        }
        return matcher.group(1);
    }

    public record ImportResult(
        String userId,
        Long emsPlaylistId,
        String externalPlaylistId,
        String sourcePlatform,
        String title,
        int trackCount,
        String collectionSource,
        Instant collectedAt
    ) {}
}
```

- [ ] **Step 5: Add explicit playlist inclusion to GMS preview**

In `GmsPlaylistPreviewService.java`, keep the existing method and delegate:

```java
public GmsPlaylistPreviewResult preview(String userId, Integer limit) {
    return preview(userId, limit, null);
}
```

Change the current `preview` body into:

```java
public GmsPlaylistPreviewResult preview(String userId, Integer limit, Long includePlaylistId) {
    // existing validation, pmsTrackCount, preferredPlatform, safeLimit, source loading...
}
```

Before sorting/limiting candidates, append the explicitly included playlist if it is not already in `source`:

```java
List<EmsCollectedPlaylistEntity> candidateSource = new ArrayList<>(source);
if (includePlaylistId != null && candidateSource.stream().noneMatch(playlist -> includePlaylistId.equals(playlist.getId()))) {
    playlistRepository.findById(includePlaylistId).ifPresent(candidateSource::add);
}

List<GmsPlaylistPreviewCandidate> candidates = candidateSource.stream()
    .filter(playlist -> playlist.getId() != null
        && !dismissedPlaylistIds.contains(playlist.getId())
        && !savedPlaylistIds.contains(playlist.getId()))
    .map(playlist -> scoreCandidate(playlist, userArtists))
    .filter(candidate -> candidate.affinityScore() > 0.0d || candidate.playlistId().equals(includePlaylistId))
    .sorted((left, right) -> {
        if (left.playlistId().equals(includePlaylistId)) {
            return -1;
        }
        if (right.playlistId().equals(includePlaylistId)) {
            return 1;
        }
        return Double.compare(right.compositeScore(), left.compositeScore());
    })
    .limit(safeLimit)
    .toList();
```

- [ ] **Step 6: Add controller endpoint and preview parameter**

Modify constructor in `GmsPlaylistPreviewController.java` to accept both services:

```java
private final GmsPlaylistPreviewService service;
private final GmsTidalPlaylistUrlImportService tidalPlaylistUrlImportService;

public GmsPlaylistPreviewController(
    GmsPlaylistPreviewService service,
    GmsTidalPlaylistUrlImportService tidalPlaylistUrlImportService
) {
    this.service = service;
    this.tidalPlaylistUrlImportService = tidalPlaylistUrlImportService;
}
```

Modify preview signature:

```java
public GmsPlaylistPreviewResponse preview(
    @RequestParam("user_id") String userId,
    @RequestParam(value = "limit", required = false) Integer limit,
    @RequestParam(value = "include_playlist_id", required = false) Long includePlaylistId
) {
    GmsPlaylistPreviewResult result = service.preview(userId, limit, includePlaylistId);
    return GmsPlaylistPreviewResponse.from(result);
}
```

Add endpoint:

```java
@Operation(summary = "Import a public TIDAL playlist URL into EMS so it can appear in GMS playlist candidates")
@PostMapping("/import/tidal-url")
public GmsTidalPlaylistUrlImportResponse importTidalPlaylistUrl(
    @RequestBody GmsTidalPlaylistUrlImportRequest request
) {
    GmsTidalPlaylistUrlImportService.ImportResult result =
        tidalPlaylistUrlImportService.importUrl(request.userId(), request.playlistUrl());
    return GmsTidalPlaylistUrlImportResponse.from(result);
}
```

Add records:

```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GmsTidalPlaylistUrlImportRequest(String userId, String playlistUrl) {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GmsTidalPlaylistUrlImportResponse(
    String service,
    String status,
    Instant generatedAt,
    String userId,
    Long emsPlaylistId,
    String externalPlaylistId,
    String sourcePlatform,
    String title,
    int trackCount,
    String collectionSource,
    Instant collectedAt
) {
    static GmsTidalPlaylistUrlImportResponse from(GmsTidalPlaylistUrlImportService.ImportResult result) {
        return new GmsTidalPlaylistUrlImportResponse(
            "api",
            "ok",
            Instant.now(),
            result.userId(),
            result.emsPlaylistId(),
            result.externalPlaylistId(),
            result.sourcePlatform(),
            result.title(),
            result.trackCount(),
            result.collectionSource(),
            result.collectedAt()
        );
    }
}
```

- [ ] **Step 7: Verify GMS tests**

Run:

```bash
./gradlew test --tests io.myforevermusic.api.modules.gms.application.GmsTidalPlaylistUrlImportServiceTest --tests io.myforevermusic.api.modules.gms.application.GmsPlaylistPreviewServiceTest
```

Expected: both test classes pass.

- [ ] **Step 8: Commit**

```bash
git add services/api/src/main/java/io/myforevermusic/api/modules/gms/application/GmsTidalPlaylistUrlImportService.java services/api/src/main/java/io/myforevermusic/api/modules/gms/application/GmsPlaylistPreviewService.java services/api/src/main/java/io/myforevermusic/api/modules/gms/presentation/GmsPlaylistPreviewController.java services/api/src/test/java/io/myforevermusic/api/modules/gms/application/GmsTidalPlaylistUrlImportServiceTest.java services/api/src/test/java/io/myforevermusic/api/modules/gms/application/GmsPlaylistPreviewServiceTest.java
git commit -m "feat: import tidal playlist urls into gms"
```

---

## Task 4: Web GMS Playlist URL Import UI

**Files:**
- Modify: `apps/web/src/types/api.ts`
- Modify: `apps/web/src/services/api.ts`
- Modify: `apps/web/src/pages/GmsPlaylistsPage.tsx`

- [ ] **Step 1: Add API types**

In `apps/web/src/types/api.ts`, add after `GmsPlaylistDismissResponse`:

```ts
export interface GmsTidalPlaylistUrlImportRequest {
    user_id: string
    playlist_url: string
}

export interface GmsTidalPlaylistUrlImportResponse {
    service: string
    status: string
    generated_at: string
    user_id: string
    ems_playlist_id: number
    external_playlist_id: string
    source_platform: string
    title: string
    track_count: number
    collection_source: string
    collected_at: string
}
```

- [ ] **Step 2: Add API wrapper**

In `apps/web/src/services/api.ts`, import the two new types and change `fetchGmsPlaylistPreview`:

```ts
export const fetchGmsPlaylistPreview = (
    userId: string,
    limit?: number,
    signal?: AbortSignal,
    includePlaylistId?: number,
) => {
    const params = new URLSearchParams({ user_id: userId })
    if (limit != null) {
        params.set('limit', String(limit))
    }
    if (includePlaylistId != null) {
        params.set('include_playlist_id', String(includePlaylistId))
    }
    return requestJson<GmsPlaylistPreviewResponse>(
        `/api/v1/gms/playlists/preview?${params.toString()}`,
        { signal },
    )
}

export const importTidalPlaylistUrlToGms = (payload: GmsTidalPlaylistUrlImportRequest) =>
    requestJson<GmsTidalPlaylistUrlImportResponse>('/api/v1/gms/playlists/import/tidal-url', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })
```

- [ ] **Step 3: Update page state and submit handler**

In `GmsPlaylistsPage.tsx`, change the first import to include the form event type:

```tsx
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
```

Add `importTidalPlaylistUrlToGms` to service imports and `GmsTidalPlaylistUrlImportResponse` to type imports.

Add state near the other `useState` calls:

```tsx
const [tidalPlaylistUrl, setTidalPlaylistUrl] = useState('')
const [isImportingTidalUrl, setIsImportingTidalUrl] = useState(false)
const [lastTidalImportResult, setLastTidalImportResult] = useState<GmsTidalPlaylistUrlImportResponse | null>(null)
const [includedPlaylistId, setIncludedPlaylistId] = useState<number | null>(null)
```

Update `loadPreview` to pass the included id:

```tsx
fetchGmsPlaylistPreview(userId, DEFAULT_LIMIT, signal, includedPlaylistId ?? undefined)
```

Add `includedPlaylistId` to the `useCallback` dependency list.

Add submit handler before `return`:

```tsx
const handleTidalPlaylistUrlImport = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!userId) {
        setErrorMessage('Sign in to import a TIDAL playlist URL.')
        return
    }
    const normalizedUrl = tidalPlaylistUrl.trim()
    if (!/^https?:\/\/(www\.)?tidal\.com\/(browse\/)?playlist\/[A-Za-z0-9][A-Za-z0-9_-]{2,159}([/?#].*)?$/.test(normalizedUrl)) {
        setErrorMessage('Enter a valid TIDAL playlist URL.')
        return
    }

    setIsImportingTidalUrl(true)
    setErrorMessage(null)
    setLastTidalImportResult(null)
    try {
        const result = await importTidalPlaylistUrlToGms({
            user_id: userId,
            playlist_url: normalizedUrl,
        })
        setLastTidalImportResult(result)
        setIncludedPlaylistId(result.ems_playlist_id)
        setTidalPlaylistUrl('')
        fetchGmsPlaylistPreview(userId, DEFAULT_LIMIT, undefined, result.ems_playlist_id)
            .then(setPreview)
            .catch((requestError: unknown) => {
                const message =
                    requestError instanceof ApiError
                        ? requestError.message
                        : 'Imported the TIDAL playlist, but could not refresh GMS candidates.'
                setErrorMessage(message)
            })
    } catch (requestError: unknown) {
        const message =
            requestError instanceof ApiError
                ? requestError.message
                : 'Unable to import this TIDAL playlist URL.'
        setErrorMessage(message)
    } finally {
        setIsImportingTidalUrl(false)
    }
}
```

- [ ] **Step 4: Add compact import form**

Inside the first `HudCard`, after the three summary cells and before `{errorMessage && (...)}`, add:

```tsx
<form
    className="mt-4 flex flex-col gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4 md:flex-row md:items-end"
    onSubmit={handleTidalPlaylistUrlImport}
>
    <label className="flex-1 text-xs font-medium uppercase tracking-[0.18em] text-hud-text-muted">
        TIDAL Playlist URL
        <input
            type="url"
            value={tidalPlaylistUrl}
            onChange={(event) => setTidalPlaylistUrl(event.target.value)}
            placeholder="https://tidal.com/playlist/..."
            className="mt-2 w-full rounded-lg border border-hud-border-secondary bg-hud-bg-secondary/70 px-3 py-2 text-sm normal-case tracking-normal text-hud-text-primary outline-none transition-hud placeholder:text-hud-text-muted focus:border-hud-accent-primary"
            disabled={isImportingTidalUrl || !userId}
        />
    </label>
    <Button
        type="submit"
        variant="primary"
        size="sm"
        disabled={isImportingTidalUrl || !userId || !tidalPlaylistUrl.trim()}
    >
        <ArrowDownToLine size={14} className={isImportingTidalUrl ? 'animate-pulse' : ''} />
        {isImportingTidalUrl ? 'Importing' : 'Import'}
    </Button>
</form>

{lastTidalImportResult && (
    <div className="mt-4 flex items-start gap-3 rounded-2xl border border-hud-accent-primary/40 bg-hud-accent-primary/10 p-4 text-sm leading-6 text-hud-text-secondary">
        <CheckCircle2 size={18} className="mt-0.5 text-hud-accent-primary" />
        <div>
            <p className="font-medium text-hud-text-primary">
                Imported TIDAL playlist: {lastTidalImportResult.title}
            </p>
            <p className="mt-1 text-xs text-hud-text-muted">
                {lastTidalImportResult.track_count} track(s) added to EMS for GMS review.
            </p>
        </div>
    </div>
)}
```

- [ ] **Step 5: Run web checks**

Run:

```bash
npm --prefix apps/web run build
```

Expected: Vite build completes without TypeScript errors.

- [ ] **Step 6: Commit**

```bash
git add apps/web/src/types/api.ts apps/web/src/services/api.ts apps/web/src/pages/GmsPlaylistsPage.tsx
git commit -m "feat: add tidal url import form to gms playlists"
```

---

## Task 5: Integrated Verification

**Files:**
- No planned code edits. Verification only.

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
./gradlew test --tests io.myforevermusic.api.modules.platform.infrastructure.tidal.TidalWebApiClientTest --tests io.myforevermusic.api.modules.ems.application.EmsCollectionServiceTest --tests io.myforevermusic.api.modules.gms.application.GmsTidalPlaylistUrlImportServiceTest --tests io.myforevermusic.api.modules.gms.application.GmsPlaylistPreviewServiceTest
```

Expected: all focused backend tests pass.

- [ ] **Step 2: Run web build**

Run:

```bash
npm --prefix apps/web run build
```

Expected: build completes.

- [ ] **Step 3: Inspect git status**

Run:

```bash
git status --short
```

Expected: only unrelated pre-existing `.planning/` remains untracked.

- [ ] **Step 4: Final implementation summary**

Report:

- Backend endpoint added: `POST /api/v1/gms/playlists/import/tidal-url`.
- GMS preview supports `include_playlist_id`.
- EMS collection source is `user_tidal_url_import`.
- Web `/gms-playlists` supports TIDAL URL import.
- Verification commands and results.
