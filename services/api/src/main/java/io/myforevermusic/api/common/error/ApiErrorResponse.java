package io.myforevermusic.api.common.error;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ApiErrorResponse(
    String service,
    String status,
    String code,
    String message,
    Instant timestamp,
    List<FieldIssue> errors
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FieldIssue(
        String field,
        String message
    ) {
    }
}
