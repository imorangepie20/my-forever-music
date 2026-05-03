package io.myforevermusic.api.modules.platform.application;

import java.util.List;

public interface PlatformConnectionStore {

    List<PlatformConnectionState> findByUserId(String userId);

    PlatformConnectionState connect(PlatformConnectionDraft draft);

    PlatformConnectionState disconnect(String userId, String platformId);
}
