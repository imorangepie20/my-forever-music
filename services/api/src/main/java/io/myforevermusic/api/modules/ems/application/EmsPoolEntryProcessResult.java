package io.myforevermusic.api.modules.ems.application;

public record EmsPoolEntryProcessResult(
    String entryType,
    int collectedPlaylistCount,
    int collectedTrackCount,
    String error
) {
    public boolean failed() {
        return error != null && !error.isBlank();
    }
}
