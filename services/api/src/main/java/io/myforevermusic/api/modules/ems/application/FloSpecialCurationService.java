package io.myforevermusic.api.modules.ems.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FloSpecialCurationService {

    private static final String FLO_ORIGIN = "https://www.music-flo.com";
    private static final String CURATIONS_PATH = "/api/personal/v1/curations/contents";
    private static final String PLAYLIST_PATH = "/api/personal/v1/playlist/";
    private static final String CHANNEL_PATH = "/api/meta/v1/channel/";
    private static final int DEFAULT_SIZE = 500;
    private static final int MAX_SECTION_LIMIT = 12;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public FloSpecialCurationService(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build());
    }

    FloSpecialCurationService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public FloSpecialCuration getSpecial(Integer limit) {
        int safeLimit = limit == null
            ? Integer.MAX_VALUE
            : Math.min(Math.max(limit, 1), MAX_SECTION_LIMIT);
        JsonNode body = requestJson(URI.create(FLO_ORIGIN + CURATIONS_PATH), "FLO special curations");
        List<FloSpecialSection> sections = new ArrayList<>();

        for (JsonNode sectionNode : body.path("data").path("list")) {
            JsonNode content = sectionNode.path("content");
            List<FloSpecialPlaylist> playlists = new ArrayList<>();
            for (JsonNode item : content.path("list")) {
                FloSpecialPlaylist playlist = toPlayableListItem(item);
                if (playlist != null) {
                    playlists.add(playlist);
                }
                if (playlists.size() >= safeLimit) {
                    break;
                }
            }
            if (!playlists.isEmpty()) {
                sections.add(new FloSpecialSection(
                    text(sectionNode, "type"),
                    text(content, "id"),
                    text(content, "title"),
                    playlists
                ));
            }
        }

        return new FloSpecialCuration(sections);
    }

    public FloSpecialPlaylistTracks getTracks(FloSpecialPlaylist playlist) {
        if ("CHNL".equalsIgnoreCase(playlist.sourceType())) {
            return getChannelTracks(playlist.externalPlaylistId());
        }
        return getPlaylistTracks(playlist.externalPlaylistId());
    }

    public FloSpecialPlaylistTracks getPlaylistTracks(String playlistId) {
        String normalizedPlaylistId = requireNumericId(playlistId);
        JsonNode body = requestJson(URI.create(FLO_ORIGIN + PLAYLIST_PATH + normalizedPlaylistId), "FLO playlist tracks");
        List<FloSpecialTrack> tracks = new ArrayList<>();

        for (JsonNode trackNode : body.path("data").path("track").path("list")) {
            tracks.add(toTrack(trackNode));
        }

        return new FloSpecialPlaylistTracks(normalizedPlaylistId, tracks.size(), tracks);
    }

    public FloSpecialPlaylistTracks getChannelTracks(String channelId) {
        String normalizedChannelId = requireNumericId(channelId);
        JsonNode body = requestJson(URI.create(FLO_ORIGIN + CHANNEL_PATH + normalizedChannelId), "FLO channel tracks");
        List<FloSpecialTrack> tracks = new ArrayList<>();

        for (JsonNode trackNode : body.path("data").path("trackList")) {
            tracks.add(toTrack(trackNode));
        }

        return new FloSpecialPlaylistTracks(normalizedChannelId, tracks.size(), tracks);
    }

    private JsonNode requestJson(URI uri, String context) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", FLO_ORIGIN + "/")
                .header("User-Agent", "MyForeverMusic/1.0 EMS FLO special")
                .header("x-gm-access-token", "")
                .header("x-gm-app-name", "FLO_WEB")
                .header("x-gm-app-version", "8.1.0")
                .header("x-gm-device-id", "MFM-EMS-FLO-SPECIAL")
                .header("x-gm-device-model", "MyForeverMusic API")
                .header("x-gm-os-type", "WEB")
                .header("x-gm-os-version", "1.0")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "%s responded with status %d.".formatted(context, response.statusCode())
                );
            }

            JsonNode body = objectMapper.readTree(response.body());
            String code = body.path("code").asText();
            if (!"2000000".equals(code)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "%s returned unexpected code %s.".formatted(context, code.isBlank() ? "unknown" : code)
                );
            }
            return body;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "%s request was interrupted.".formatted(context), exception);
        } catch (ConnectException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "%s endpoint is unreachable.".formatted(context), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "%s returned an unreadable response.".formatted(context), exception);
        }
    }

    private static FloSpecialPlaylist toPlayableListItem(JsonNode item) {
        String type = text(item, "type");
        if ("PLAYLIST".equalsIgnoreCase(type)) {
            return toPlaylist(item);
        }
        if ("CHNL".equalsIgnoreCase(type)) {
            return toChannel(item);
        }
        return null;
    }

    private static FloSpecialPlaylist toPlaylist(JsonNode item) {
        List<String> coverUrls = imageUrls(item.path("gridImg"), DEFAULT_SIZE);
        String playlistId = text(item, "id");
        return new FloSpecialPlaylist(
            playlistId,
            text(item, "name"),
            text(item, "type"),
            firstOrNull(coverUrls),
            coverUrls,
            FLO_ORIGIN + "/detail/playlist/" + playlistId
        );
    }

    private static FloSpecialPlaylist toChannel(JsonNode item) {
        List<String> coverUrls = imageUrls(item.path("img"), DEFAULT_SIZE);
        String channelId = text(item, "id");
        return new FloSpecialPlaylist(
            channelId,
            text(item, "name"),
            text(item, "type"),
            firstOrNull(coverUrls),
            coverUrls,
            FLO_ORIGIN + "/detail/channel/" + channelId
        );
    }

    private static FloSpecialTrack toTrack(JsonNode trackNode) {
        JsonNode album = trackNode.path("album");
        String trackId = text(trackNode, "id");
        return new FloSpecialTrack(
            trackId,
            text(trackNode, "name"),
            artistName(trackNode),
            text(album, "title"),
            firstOrNull(imageUrls(album.path("img"), DEFAULT_SIZE)),
            FLO_ORIGIN + "/detail/track/" + trackId,
            parsePlayTime(text(trackNode, "playTime")),
            text(album, "releaseYmd")
        );
    }

    private static List<String> imageUrls(JsonNode imageNode, int size) {
        List<String> urls = new ArrayList<>();
        JsonNode urlFormatList = imageNode.path("urlFormatList");
        if (urlFormatList.isArray()) {
            for (JsonNode value : urlFormatList) {
                String url = replaceSize(value.asText(null), size);
                if (url != null && !url.isBlank()) {
                    urls.add(url);
                }
            }
        }

        String singleUrl = replaceSize(imageNode.path("urlFormat").asText(null), size);
        if (singleUrl != null && !singleUrl.isBlank()) {
            urls.add(singleUrl);
        }
        return urls;
    }

    private static String artistName(JsonNode trackNode) {
        List<String> artists = new ArrayList<>();
        for (JsonNode artist : trackNode.path("artistList")) {
            String name = text(artist, "name");
            if (!name.isBlank()) {
                artists.add(name);
            }
        }
        if (!artists.isEmpty()) {
            return String.join(", ", artists);
        }
        return text(trackNode.path("representationArtist"), "name");
    }

    private static Integer parsePlayTime(String playTime) {
        if (playTime == null || playTime.isBlank()) {
            return null;
        }
        String[] parts = playTime.split(":");
        int seconds = 0;
        for (String part : parts) {
            try {
                seconds = seconds * 60 + Integer.parseInt(part);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return seconds * 1000;
    }

    private static String requireNumericId(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FLO playlist id is required.");
        }
        String trimmed = value.trim();
        if (!trimmed.matches("[0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FLO playlist id must be numeric.");
        }
        return trimmed;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private static String replaceSize(String urlFormat, int size) {
        if (urlFormat == null || urlFormat.isBlank()) {
            return null;
        }
        return urlFormat.replace("{size}", String.valueOf(size));
    }

    private static String firstOrNull(List<String> values) {
        return values.isEmpty() ? null : values.getFirst();
    }

    public record FloSpecialCuration(
        List<FloSpecialSection> sections
    ) {}

    public record FloSpecialSection(
        String sourceType,
        String externalSectionId,
        String title,
        List<FloSpecialPlaylist> playlists
    ) {}

    public record FloSpecialPlaylist(
        String externalPlaylistId,
        String title,
        String sourceType,
        String coverImageUrl,
        List<String> coverImageUrls,
        String platformExternalUrl
    ) {}

    public record FloSpecialPlaylistTracks(
        String externalPlaylistId,
        int trackCount,
        List<FloSpecialTrack> tracks
    ) {}

    public record FloSpecialTrack(
        String externalTrackId,
        String title,
        String artistName,
        String albumTitle,
        String albumImageUrl,
        String platformExternalUrl,
        Integer durationMs,
        String releaseYmd
    ) {}
}
