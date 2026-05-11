package io.myforevermusic.api.modules.recommendation.presentation;

import io.myforevermusic.api.modules.recommendation.application.UserMusicEventService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/events")
public class UserMusicEventController {

    private final UserMusicEventService eventService;

    public UserMusicEventController(UserMusicEventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Record a user music behavior event for recommendation learning")
    @PostMapping
    public UserMusicEventResponse recordEvent(@Valid @RequestBody UserMusicEventRequest request) {
        return eventService.recordEvent(request);
    }
}
