package io.myforevermusic.api.modules.pms.presentation;

import io.myforevermusic.api.modules.pms.application.PmsPersonalPlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/pms/personal-playlists")
public class PmsPersonalPlaylistController {

    private final PmsPersonalPlaylistService personalPlaylistService;

    public PmsPersonalPlaylistController(PmsPersonalPlaylistService personalPlaylistService) {
        this.personalPlaylistService = personalPlaylistService;
    }

    @Operation(summary = "Load PMS personal playlists")
    @GetMapping("/bootstrap")
    public PmsPersonalPlaylistBootstrapResponse bootstrap(
        @RequestParam("user_id") @NotBlank(message = "user_id is required.") String userId
    ) {
        return personalPlaylistService.bootstrap(userId);
    }

    @Operation(summary = "Create a PMS personal playlist")
    @PostMapping
    public PmsPersonalPlaylistCommandResponse createPlaylist(
        @Valid @RequestBody PmsPersonalPlaylistCreateRequest request
    ) {
        return personalPlaylistService.createPlaylist(request);
    }

    @Operation(summary = "Save a PMS library track into a personal playlist")
    @PostMapping("/tracks")
    public PmsPersonalPlaylistCommandResponse saveTrack(
        @Valid @RequestBody PmsPersonalPlaylistTrackSaveRequest request
    ) {
        return personalPlaylistService.saveTrack(request);
    }
}
