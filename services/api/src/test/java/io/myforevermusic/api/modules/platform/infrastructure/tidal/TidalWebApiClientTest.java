package io.myforevermusic.api.modules.platform.infrastructure.tidal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.myforevermusic.api.modules.platform.application.PlatformAccountCredential;
import io.myforevermusic.api.modules.platform.application.PlatformOAuthProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TidalWebApiClientTest {

    @Test
    void shouldParsePlaylistTracksWithIncludedArtistsAlbumsAndIsrc() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/playlists/playlist-001", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.contains("include=items,items.artists,items.albums")) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            byte[] response = """
                {
                  "data": {
                    "id": "playlist-001",
                    "type": "playlists",
                    "attributes": {
                      "creatorId": "user-001"
                    }
                  },
                  "included": [
                    {
                      "id": "item-001",
                      "type": "items",
                      "relationships": {
                        "track": {
                          "data": {
                            "id": "track-001",
                            "type": "tracks"
                          }
                        }
                      }
                    },
                    {
                      "id": "track-001",
                      "type": "tracks",
                      "attributes": {
                        "title": "Midnight Receiver",
                        "duration": 218,
                        "isrc": "USRC17607839",
                        "url": "https://tidal.com/browse/track/track-001",
                        "previewUrl": "https://cdn.tidal.com/preview/track-001.mp3"
                      },
                      "relationships": {
                        "artists": {
                          "data": [
                            {"id": "artist-001", "type": "artists"},
                            {"id": "artist-002", "type": "artists"}
                          ]
                        },
                        "albums": {
                          "data": [
                            {"id": "album-001", "type": "albums"}
                          ]
                        }
                      }
                    },
                    {
                      "id": "artist-001",
                      "type": "artists",
                      "attributes": {
                        "name": "Neon Bloom"
                      }
                    },
                    {
                      "id": "artist-002",
                      "type": "artists",
                      "attributes": {
                        "name": "Aurora Lane"
                      }
                    },
                    {
                      "id": "album-001",
                      "type": "albums",
                      "attributes": {
                        "title": "Signal Bloom",
                        "imageId": "ab12cd34-ef56-7890-ab12-cd34ef567890"
                      }
                    }
                  ]
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

            List<TidalWebApiClient.TidalPlaylistTrack> tracks = client.getPlaylistTracks(
                tidalCredential(),
                "playlist-001"
            );

            assertThat(tracks).hasSize(1);
            assertThat(tracks.get(0).tidalTrackId()).isEqualTo("track-001");
            assertThat(tracks.get(0).title()).isEqualTo("Midnight Receiver");
            assertThat(tracks.get(0).artistName()).isEqualTo("Neon Bloom, Aurora Lane");
            assertThat(tracks.get(0).albumTitle()).isEqualTo("Signal Bloom");
            assertThat(tracks.get(0).externalUrl()).isEqualTo("https://tidal.com/browse/track/track-001");
            assertThat(tracks.get(0).previewUrl()).isEqualTo("https://cdn.tidal.com/preview/track-001.mp3");
            assertThat(tracks.get(0).tidalUri()).isEqualTo("tidal:track:track-001");
            assertThat(tracks.get(0).isrc()).isEqualTo("USRC17607839");
            assertThat(tracks.get(0).durationMs()).isEqualTo(218000);
            assertThat(tracks.get(0).albumImageUrl())
                .isEqualTo("https://resources.tidal.com/images/ab12cd34/ef56/7890/ab12/cd34ef567890/750x750.jpg");
        } finally {
            server.stop(0);
        }
    }

    private PlatformAccountCredential tidalCredential() {
        return new PlatformAccountCredential(
            "user-001",
            "tidal",
            "tidal-pkce-draft",
            "tidal-user-001",
            "Forever Listener TIDAL account",
            "tidal-access-token",
            "tidal-refresh-token",
            "Bearer",
            "playlist-read-private",
            Instant.parse("2026-05-04T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
    }
}
