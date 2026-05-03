package io.myforevermusic.api.modules.platform.application;

public class PlatformReconnectRequiredException extends RuntimeException {

    private final String platformId;

    public PlatformReconnectRequiredException(String platformId, String message) {
        super(message);
        this.platformId = platformId;
    }

    public String getPlatformId() {
        return platformId;
    }
}
