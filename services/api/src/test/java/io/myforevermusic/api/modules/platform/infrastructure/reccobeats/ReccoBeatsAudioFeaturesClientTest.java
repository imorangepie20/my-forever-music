package io.myforevermusic.api.modules.platform.infrastructure.reccobeats;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsAudioFeaturesSnapshot;
import io.myforevermusic.api.modules.platform.infrastructure.reccobeats.ReccoBeatsAudioFeaturesClient.ReccoBeatsTrackLookupRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReccoBeatsAudioFeaturesClientTest {

    @Test
    void shouldResolveSpotifyTrackIdsFromReccoBeatsAudioFeaturesResponse() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/audio-features", exchange -> {
            byte[] response = """
                {
                  "content": [
                    {
                      "id": "8212bab8-5911-48a0-b177-24923ef2329a",
                      "href": "https://open.spotify.com/track/00aqkszH1FdUiJJWvX6iEl",
                      "isrc": "USUM72104140",
                      "acousticness": 0.123,
                      "danceability": 0.654,
                      "energy": 0.789,
                      "instrumentalness": 0.001,
                      "key": 5,
                      "liveness": 0.111,
                      "loudness": -8.2,
                      "mode": 1,
                      "speechiness": 0.045,
                      "tempo": 121.4,
                      "valence": 0.612
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            ReccoBeatsProperties properties = new ReccoBeatsProperties();
            properties.setBaseUrl("http://127.0.0.1:%d/v1".formatted(server.getAddress().getPort()));
            ReccoBeatsAudioFeaturesClient client = new ReccoBeatsAudioFeaturesClient(
                properties,
                new ObjectMapper()
            );

            Map<String, ReccoBeatsAudioFeaturesSnapshot> snapshots = client.getAudioFeaturesForSpotifyTrackIds(
                List.of("00aqkszH1FdUiJJWvX6iEl")
            );

            assertThat(snapshots).containsOnlyKeys("00aqkszH1FdUiJJWvX6iEl");
            assertThat(snapshots.get("00aqkszH1FdUiJJWvX6iEl").reccoBeatsTrackId())
                .isEqualTo("8212bab8-5911-48a0-b177-24923ef2329a");
            assertThat(snapshots.get("00aqkszH1FdUiJJWvX6iEl").spotifyTrackHref())
                .isEqualTo("https://open.spotify.com/track/00aqkszH1FdUiJJWvX6iEl");
            assertThat(snapshots.get("00aqkszH1FdUiJJWvX6iEl").musicalKey()).isEqualTo(5);
            assertThat(snapshots.get("00aqkszH1FdUiJJWvX6iEl").mode()).isEqualTo(1);
            assertThat(snapshots.get("00aqkszH1FdUiJJWvX6iEl").tempo()).isEqualTo(121.4);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldChooseBestReccoBeatsTrackCandidateForIsrcLookup() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/track", exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/audio-features")) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            byte[] response = """
                {
                  "content": [
                    {
                      "id": "recco-uuid-001",
                      "trackTitle": "Midnight Receiver",
                      "artists": [{"name": "Neon Bloom"}],
                      "durationMs": 218000,
                      "isrc": "USRC17607839",
                      "href": "https://open.spotify.com/track/spotify-track-001",
                      "popularity": 41
                    },
                    {
                      "id": "recco-uuid-002",
                      "trackTitle": "Midnight Receiver",
                      "artists": [{"name": "Different Artist"}],
                      "durationMs": 231000,
                      "isrc": "USRC17607839",
                      "href": "https://open.spotify.com/track/spotify-track-999",
                      "popularity": 90
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/v1/track/recco-uuid-001/audio-features", exchange -> {
            byte[] response = """
                {
                  "id": "recco-uuid-001",
                  "href": "https://open.spotify.com/track/spotify-track-001",
                  "isrc": "USRC17607839",
                  "acousticness": 0.211,
                  "danceability": 0.702,
                  "energy": 0.744,
                  "instrumentalness": 0.013,
                  "key": 8,
                  "liveness": 0.094,
                  "loudness": -8.7,
                  "mode": 1,
                  "speechiness": 0.039,
                  "tempo": 118.4,
                  "valence": 0.58
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/v1/track/recco-uuid-002/audio-features", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        try {
            ReccoBeatsProperties properties = new ReccoBeatsProperties();
            properties.setBaseUrl("http://127.0.0.1:%d/v1".formatted(server.getAddress().getPort()));
            ReccoBeatsAudioFeaturesClient client = new ReccoBeatsAudioFeaturesClient(
                properties,
                new ObjectMapper()
            );

            Map<String, ReccoBeatsAudioFeaturesSnapshot> snapshots = client.getAudioFeaturesForExternalTracksByIsrc(
                List.of(new ReccoBeatsTrackLookupRequest(
                    "tidal-track-001",
                    "Midnight Receiver",
                    "Neon Bloom",
                    218000,
                    "USRC17607839"
                ))
            );

            assertThat(snapshots).containsOnlyKeys("tidal-track-001");
            assertThat(snapshots.get("tidal-track-001").reccoBeatsTrackId()).isEqualTo("recco-uuid-001");
            assertThat(snapshots.get("tidal-track-001").spotifyTrackId()).isEqualTo("spotify-track-001");
            assertThat(snapshots.get("tidal-track-001").tempo()).isEqualTo(118.4);
        } finally {
            server.stop(0);
        }
    }
}
