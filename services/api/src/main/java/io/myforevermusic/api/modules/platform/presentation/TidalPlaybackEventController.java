package io.myforevermusic.api.modules.platform.presentation;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platforms/playback/tidal/events")
public class TidalPlaybackEventController {

    private static final Pattern EVENT_ID_PARAM = Pattern.compile("^SendMessageBatchRequestEntry\\.(\\d+)\\.Id$");

    @Operation(summary = "Accept TIDAL playback event batches")
    @PostMapping(
        value = "/batch",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String acceptPlaybackEventBatch(@RequestParam MultiValueMap<String, String> form) {
        return buildBatchResponse(readEventIds(form));
    }

    @Operation(summary = "Accept public TIDAL playback event batches")
    @PostMapping(
        value = "/public-batch",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE
    )
    public String acceptPublicPlaybackEventBatch(@RequestParam MultiValueMap<String, String> form) {
        return buildBatchResponse(readEventIds(form));
    }

    private List<String> readEventIds(MultiValueMap<String, String> form) {
        return form.entrySet().stream()
            .map(entry -> new IndexedEventId(entry.getKey(), firstValue(entry.getValue())))
            .filter(IndexedEventId::valid)
            .sorted(Comparator.comparingInt(IndexedEventId::index))
            .map(IndexedEventId::id)
            .distinct()
            .toList();
    }

    private String firstValue(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.get(0);
    }

    private String buildBatchResponse(List<String> eventIds) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<SendMessageBatchResponse>");
        xml.append("<SendMessageBatchResult>");
        eventIds.forEach(eventId -> {
            String escapedId = escapeXml(eventId);
            xml.append("<SendMessageBatchResultEntry>");
            xml.append("<Id>").append(escapedId).append("</Id>");
            xml.append("<MessageId>").append(escapedId).append("</MessageId>");
            xml.append("<MD5OfMessageBody>00000000000000000000000000000000</MD5OfMessageBody>");
            xml.append("</SendMessageBatchResultEntry>");
        });
        xml.append("</SendMessageBatchResult>");
        xml.append("<ResponseMetadata><RequestId>tidal-playback-event-batch</RequestId></ResponseMetadata>");
        xml.append("</SendMessageBatchResponse>");
        return xml.toString();
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private record IndexedEventId(int index, String id) {
        private IndexedEventId(String key, String id) {
            this(readIndex(key), id == null ? "" : id.trim());
        }

        private boolean valid() {
            return index > 0 && !id.isBlank();
        }

        private static int readIndex(String key) {
            Matcher matcher = EVENT_ID_PARAM.matcher(key);
            if (!matcher.matches()) {
                return -1;
            }

            return Integer.parseInt(matcher.group(1));
        }
    }
}
