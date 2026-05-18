package io.myforevermusic.api.modules.gms.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.myforevermusic.api.modules.ems.application.EmsCollectionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GmsTidalPlaylistUrlImportServiceTest {

    @Test
    void shouldImportValidTidalPlaylistUrl() {
        EmsCollectionService emsCollectionService = mock(EmsCollectionService.class);
        GmsTidalPlaylistUrlImportService service = new GmsTidalPlaylistUrlImportService(emsCollectionService);
        when(emsCollectionService.collectTidalPlaylistFromUrlImport(
            "user-001",
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
        )).thenReturn(new EmsCollectionService.EmsTidalPlaylistUrlImportCollection(
            2L,
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33",
            "tidal",
            "TIDAL Import",
            12,
            "user_tidal_url_import",
            Instant.parse("2026-05-18T00:00:00Z")
        ));

        GmsTidalPlaylistUrlImportService.ImportResult result = service.importUrl(
            "user-001",
            "https://tidal.com/browse/playlist/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
        );

        assertThat(result.emsPlaylistId()).isEqualTo(2L);
        assertThat(result.externalPlaylistId()).isEqualTo("0a3d87d2-27dc-4edc-84b6-9f1eaa567f33");
        verify(emsCollectionService).collectTidalPlaylistFromUrlImport(
            "user-001",
            "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
        );
    }

    @Test
    void shouldRejectNonTidalUrl() {
        EmsCollectionService emsCollectionService = mock(EmsCollectionService.class);
        GmsTidalPlaylistUrlImportService service = new GmsTidalPlaylistUrlImportService(emsCollectionService);

        assertThatThrownBy(() -> service.importUrl("user-001", "https://example.com/playlist/not-tidal"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("TIDAL playlist URL");
    }
}
