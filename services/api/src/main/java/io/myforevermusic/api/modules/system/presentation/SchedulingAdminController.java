package io.myforevermusic.api.modules.system.presentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.myforevermusic.api.modules.system.application.SchedulingAdminService;
import io.myforevermusic.api.modules.system.application.SchedulingAdminService.ScheduledServiceStatus;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/admin/schedules")
public class SchedulingAdminController {

    private final SchedulingAdminService schedulingAdminService;

    public SchedulingAdminController(SchedulingAdminService schedulingAdminService) {
        this.schedulingAdminService = schedulingAdminService;
    }

    @Operation(summary = "Get scheduler cadence and health for admin operations")
    @GetMapping
    public SchedulingAdminResponse getSchedules(@RequestParam("user_id") String userId) {
        return SchedulingAdminResponse.from(schedulingAdminService.summarize(userId));
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SchedulingAdminResponse(
        String service,
        String status,
        Instant generatedAt,
        List<ScheduledServiceItem> schedules,
        List<String> recommendations
    ) {
        static SchedulingAdminResponse from(SchedulingAdminService.SchedulingAdminReport report) {
            return new SchedulingAdminResponse(
                "api",
                report.status(),
                report.generatedAt(),
                report.schedules().stream().map(ScheduledServiceItem::from).toList(),
                report.recommendations()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ScheduledServiceItem(
        String id,
        String domain,
        String name,
        String mode,
        boolean enabled,
        boolean configured,
        String status,
        Long fixedDelayMs,
        Long initialDelayMs,
        String cadenceLabel,
        String purpose,
        String managementPath,
        String lastStatus,
        String lastMessage,
        Instant lastStartedAt,
        Instant lastCompletedAt,
        List<String> configKeys,
        List<String> notes
    ) {
        static ScheduledServiceItem from(ScheduledServiceStatus schedule) {
            return new ScheduledServiceItem(
                schedule.id(),
                schedule.domain(),
                schedule.name(),
                schedule.mode(),
                schedule.enabled(),
                schedule.configured(),
                schedule.status(),
                schedule.fixedDelayMs(),
                schedule.initialDelayMs(),
                schedule.cadenceLabel(),
                schedule.purpose(),
                schedule.managementPath(),
                schedule.lastStatus(),
                schedule.lastMessage(),
                schedule.lastStartedAt(),
                schedule.lastCompletedAt(),
                schedule.configKeys(),
                schedule.notes()
            );
        }
    }
}
