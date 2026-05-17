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
    private static final Pattern OG_DESCRIPTION = Pattern.compile(
        "<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TWITTER_DESCRIPTION = Pattern.compile(
        "<meta[^>]+name=[\"']twitter:description[\"'][^>]+content=[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern META_DESCRIPTION = Pattern.compile(
        "<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HTML_ENTITY = Pattern.compile("&(#\\d+|#x[0-9a-fA-F]+|[a-zA-Z]+);");
    private static final int HTML_BODY_LIMIT = 200_000;
    private static final int METADATA_CACHE_SIZE = 256;
    private static final int TRANSLATION_CACHE_SIZE = 512;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, ArticleMetadata> metadataCache = Collections.synchronizedMap(new LinkedHashMap<>(METADATA_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ArticleMetadata> eldest) {
            return size() > METADATA_CACHE_SIZE;
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
        ArticleMetadata metadata = loadMetadata(articleUrl);
        return metadata == null ? null : metadata.imageUrl();
    }

    public String fetchDescription(String articleUrl) {
        ArticleMetadata metadata = loadMetadata(articleUrl);
        return metadata == null ? null : metadata.description();
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

    private ArticleMetadata loadMetadata(String articleUrl) {
        if (articleUrl == null || articleUrl.isBlank()) {
            return null;
        }
        ArticleMetadata cached = metadataCache.get(articleUrl);
        if (cached != null) {
            return cached;
        }
        ArticleMetadata fetched = scrape(articleUrl);
        metadataCache.put(articleUrl, fetched == null ? ArticleMetadata.EMPTY : fetched);
        return fetched;
    }

    private ArticleMetadata scrape(String articleUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(articleUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", "MyForeverMusic/1.0 magazine-enricher")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ArticleMetadata.EMPTY;
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                return ArticleMetadata.EMPTY;
            }
            if (body.length() > HTML_BODY_LIMIT) {
                body = body.substring(0, HTML_BODY_LIMIT);
            }
            return new ArticleMetadata(
                firstMatch(body, OG_IMAGE, TWITTER_IMAGE),
                firstMatch(body, OG_DESCRIPTION, TWITTER_DESCRIPTION, META_DESCRIPTION)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ArticleMetadata.EMPTY;
        } catch (RuntimeException | java.io.IOException exception) {
            log.debug("Magazine metadata scrape failed for {}: {}", articleUrl, exception.getMessage());
            return ArticleMetadata.EMPTY;
        }
    }

    private static String firstMatch(String html, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String raw = matcher.group(1).trim();
                if (!raw.isEmpty()) {
                    return decodeEntities(raw);
                }
            }
        }
        return null;
    }

    private static String decodeEntities(String value) {
        Matcher matcher = HTML_ENTITY.matcher(value);
        StringBuilder builder = new StringBuilder(value.length());
        int last = 0;
        while (matcher.find()) {
            builder.append(value, last, matcher.start());
            builder.append(resolveEntity(matcher.group(1)));
            last = matcher.end();
        }
        builder.append(value, last, value.length());
        return builder.toString();
    }

    private static String resolveEntity(String token) {
        if (token.startsWith("#x") || token.startsWith("#X")) {
            try {
                int codePoint = Integer.parseInt(token.substring(2), 16);
                return new String(Character.toChars(codePoint));
            } catch (NumberFormatException ignored) {
                return "&" + token + ";";
            }
        }
        if (token.startsWith("#")) {
            try {
                int codePoint = Integer.parseInt(token.substring(1));
                return new String(Character.toChars(codePoint));
            } catch (NumberFormatException ignored) {
                return "&" + token + ";";
            }
        }
        return switch (token) {
            case "amp" -> "&";
            case "lt" -> "<";
            case "gt" -> ">";
            case "quot" -> "\"";
            case "apos" -> "'";
            case "nbsp" -> " ";
            case "rsquo", "lsquo" -> "'";
            case "rdquo", "ldquo" -> "\"";
            case "hellip" -> "…";
            case "mdash" -> "—";
            case "ndash" -> "–";
            default -> "&" + token + ";";
        };
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

    private record ArticleMetadata(String imageUrl, String description) {
        static final ArticleMetadata EMPTY = new ArticleMetadata(null, null);
    }
}
