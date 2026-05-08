package io.myforevermusic.api.modules.pms.presentation;

import io.myforevermusic.api.modules.pms.application.PmsPlaylistDetailService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pms/playlists")
@Validated
public class PmsPlaylistDetailController {

    private final PmsPlaylistDetailService pmsPlaylistDetailService;

    public PmsPlaylistDetailController(PmsPlaylistDetailService pmsPlaylistDetailService) {
        this.pmsPlaylistDetailService = pmsPlaylistDetailService;
    }

    @Operation(summary = "Get full PMS playlist detail and ordered tracks")
    @GetMapping("/{playlist_id}")
    public PmsPlaylistDetailResponse getPlaylistDetail(
        @RequestParam("user_id") @NotBlank String userId,
        @PathVariable("playlist_id") @NotBlank String playlistId
    ) {
        return pmsPlaylistDetailService.getPlaylistDetail(userId, playlistId);
    }
}
