package io.myforevermusic.api.modules.ems.infrastructure.source;

import io.myforevermusic.api.modules.ems.application.EmsEditorialArticle;
import io.myforevermusic.api.modules.ems.application.EmsEditorialSource;
import io.myforevermusic.api.modules.ems.application.EmsEditorialSourceClient;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class RssEmsEditorialSourceClient implements EmsEditorialSourceClient {

    private final HttpClient httpClient;

    public RssEmsEditorialSourceClient() {
        this(HttpClient.newHttpClient());
    }

    RssEmsEditorialSourceClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public List<EmsEditorialArticle> fetch(EmsEditorialSource source, int limit) {
        if (!"rss".equalsIgnoreCase(source.type()) && !"atom".equalsIgnoreCase(source.type())) {
            throw new IllegalArgumentException("Unsupported EMS editorial source type: %s".formatted(source.type()));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source.url()))
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                .header("User-Agent", "MyForeverMusic/1.0 EMS acquisition")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                    "EMS editorial source responded with status %d.".formatted(response.statusCode())
                );
            }
            return parseFeed(source, response.body(), limit);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("EMS editorial source fetch was interrupted.", exception);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("EMS editorial source fetch failed: %s".formatted(exception.getMessage()), exception);
        }
    }

    static List<EmsEditorialArticle> parseFeed(EmsEditorialSource source, String xml, int limit) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            List<EmsEditorialArticle> rssItems = parseRssItems(source, document, limit);
            if (!rssItems.isEmpty()) {
                return rssItems;
            }
            return parseAtomEntries(source, document, limit);
        } catch (Exception exception) {
            throw new IllegalStateException("EMS editorial source returned invalid RSS/Atom XML.", exception);
        }
    }

    private static List<EmsEditorialArticle> parseRssItems(EmsEditorialSource source, Document document, int limit) {
        NodeList items = document.getElementsByTagName("item");
        List<EmsEditorialArticle> articles = new ArrayList<>();
        for (int i = 0; i < items.getLength() && articles.size() < limit; i++) {
            if (items.item(i) instanceof Element item) {
                String title = text(item, "title");
                if (title.isBlank()) {
                    continue;
                }
                articles.add(new EmsEditorialArticle(
                    source.name(),
                    source.url(),
                    text(item, "link"),
                    title,
                    firstNonBlank(text(item, "description"), text(item, "summary")),
                    parseInstant(firstNonBlank(text(item, "pubDate"), text(item, "updated")))
                ));
            }
        }
        return articles;
    }

    private static List<EmsEditorialArticle> parseAtomEntries(EmsEditorialSource source, Document document, int limit) {
        NodeList entries = document.getElementsByTagNameNS("*", "entry");
        List<EmsEditorialArticle> articles = new ArrayList<>();
        for (int i = 0; i < entries.getLength() && articles.size() < limit; i++) {
            if (entries.item(i) instanceof Element entry) {
                String title = textNs(entry, "title");
                if (title.isBlank()) {
                    continue;
                }
                articles.add(new EmsEditorialArticle(
                    source.name(),
                    source.url(),
                    atomLink(entry),
                    title,
                    firstNonBlank(textNs(entry, "summary"), textNs(entry, "content")),
                    parseInstant(firstNonBlank(textNs(entry, "published"), textNs(entry, "updated")))
                ));
            }
        }
        return articles;
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String textNs(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String atomLink(Element entry) {
        NodeList links = entry.getElementsByTagNameNS("*", "link");
        for (int i = 0; i < links.getLength(); i++) {
            if (links.item(i) instanceof Element link) {
                String href = link.getAttribute("href");
                if (!href.isBlank()) {
                    return href.trim();
                }
            }
        }
        return "";
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second == null ? "" : second;
    }
}
