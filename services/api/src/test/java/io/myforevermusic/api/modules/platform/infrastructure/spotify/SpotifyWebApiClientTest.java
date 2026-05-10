package io.myforevermusic.api.modules.platform.infrastructure.spotify;

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

class SpotifyWebApiClientTest {

    @Test
    void shouldParseFeaturedPlaylists() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/browse/featured-playlists", exchange -> {
            byte[] response = playlistResponse("Featured Rotation").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            SpotifyWebApiClient client = client(server);

            SpotifyWebApiClient.SpotifySearchResult<SpotifyWebApiClient.SpotifyPlaylistSummary> playlists =
                client.getFeaturedPlaylists(spotifyCredential(), 5);

            assertThat(playlists.items()).hasSize(1);
            assertThat(playlists.items().get(0).playlistId()).isEqualTo("playlist-001");
            assertThat(playlists.items().get(0).name()).isEqualTo("Featured Rotation");
            assertThat(playlists.items().get(0).ownerDisplayName()).isEqualTo("Spotify");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldParseCategoryPlaylists() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/browse/categories/toplists/playlists", exchange -> {
            byte[] response = playlistResponse("Top Lists").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            SpotifyWebApiClient client = client(server);

            SpotifyWebApiClient.SpotifySearchResult<SpotifyWebApiClient.SpotifyPlaylistSummary> playlists =
                client.getCategoryPlaylists(spotifyCredential(), "toplists", 5);

            assertThat(playlists.items()).hasSize(1);
            assertThat(playlists.items().get(0).name()).isEqualTo("Top Lists");
        } finally {
            server.stop(0);
        }
    }

    private SpotifyWebApiClient client(HttpServer server) {
        PlatformOAuthProperties properties = new PlatformOAuthProperties();
        properties.getSpotify().setApiBaseUri("http://127.0.0.1:%d/v1".formatted(server.getAddress().getPort()));
        return new SpotifyWebApiClient(properties, new ObjectMapper(), HttpClient.newHttpClient());
    }

    private String playlistResponse(String title) {
        return """
            {
              "message": "Popular Playlists",
              "playlists": {
                "total": 1,
                "items": [
                  {
                    "id": "playlist-001",
                    "name": "%s",
                    "description": "Curated playlist.",
                    "uri": "spotify:playlist:playlist-001",
                    "external_urls": {
                      "spotify": "https://open.spotify.com/playlist/playlist-001"
                    },
                    "owner": {
                      "id": "spotify",
                      "display_name": "Spotify"
                    },
                    "tracks": {
                      "total": 50
                    },
                    "images": [
                      {
                        "url": "https://image.example/playlist.jpg"
                      }
                    ]
                  }
                ]
              }
            }
            """.formatted(title);
    }

    private PlatformAccountCredential spotifyCredential() {
        return new PlatformAccountCredential(
            "user-001",
            "spotify",
            "spotify-pkce-draft",
            "spotify-user-001",
            "Forever Listener Spotify account",
            "spotify-access-token",
            "spotify-refresh-token",
            "Bearer",
            "playlist-read-private",
            Instant.parse("2026-05-04T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z"),
            Instant.parse("2026-05-03T00:00:00Z")
        );
    }
}
