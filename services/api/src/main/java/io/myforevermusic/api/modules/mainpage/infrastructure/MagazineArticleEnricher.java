package io.myforevermusic.api.modules.mainpage.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MagazineArticleEnricher {

    private static final Logger log = LoggerFactory.getLogger(MagazineArticleEnricher.class);
    private static final Pattern OG_IMAGE = Pattern.compile(
        "<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TWITTER_IMAGE = Pattern.compile(
        "<meta[^>]+name=[\"']twitter:image[\"'][^>]+content=[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    private static final int IMAGE_BODY_LIMIT = 200_000;
    private static final int IMAGE_CACHE_SIZE = 256;
    private static final int TRANSLATION_CACHE_SIZE = 512;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> imageCache = Collections.synchronizedMap(new LinkedHashMap<>(IMAGE_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > IMAGE_CACHE_SIZE;
        }
    });
    private final Map<String, String> translationCache = Collections.synchronizedMap(new LinkedHashMap<>(TRANSLATION_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > TRANSLATION_CACHE_SIZE;
        }
    });

    @Autowired
    public MagazineArticleEnricher(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build());
    }

    MagazineArticleEnricher(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public String fetchImageUrl(String articleUrl) {
        if (articleUrl == null || articleUrl.isBlank()) {
            return null;
        }
        String cached = imageCache.get(articleUrl);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        String resolved = scrape(articleUrl);
        imageCache.put(articleUrl, resolved == null ? "" : resolved);
        return resolved;
    }

    public String translateToKorean(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cached = translationCache.get(text);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        String translated = callGoogleTranslate(text);
        translationCache.put(text, translated == null ? "" : translated);
        return translated;
    }

    private String scrape(String articleUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(articleUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", "MyForeverMusic/1.0 magazine-thumbnail")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                return null;
            }
            if (body.length() > IMAGE_BODY_LIMIT) {
                body = body.substring(0, IMAGE_BODY_LIMIT);
            }
            Matcher og = OG_IMAGE.matcher(body);
            if (og.find()) {
                return og.group(1).trim();
            }
            Matcher tw = TWITTER_IMAGE.matcher(body);
            if (tw.find()) {
                return tw.group(1).trim();
            }
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (RuntimeException | java.io.IOException exception) {
            log.debug("Magazine image scrape failed for {}: {}", articleUrl, exception.getMessage());
            return null;
        }
    }

    private String callGoogleTranslate(String text) {
        try {
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ko&dt=t&q="
                + URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json")
                .header("User-Agent", "MyForeverMusic/1.0 magazine-translate")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            JsonNode body = objectMapper.readTree(response.body());
            JsonNode segments = body.path(0);
            if (!segments.isArray()) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            for (JsonNode segment : segments) {
                JsonNode value = segment.path(0);
                if (value.isTextual()) {
                    builder.append(value.asText());
                }
            }
            String result = builder.toString().trim();
            return result.isBlank() ? null : result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (RuntimeException | java.io.IOException exception) {
            log.debug("Magazine translation failed for text='{}': {}",
                text.length() > 60 ? text.substring(0, 60) + "…" : text,
                exception.getMessage());
            return null;
        }
    }
}
