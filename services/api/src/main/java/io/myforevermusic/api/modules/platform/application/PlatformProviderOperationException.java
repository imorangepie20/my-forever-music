package io.myforevermusic.api.modules.platform.application;

public class PlatformProviderOperationException extends RuntimeException {

    private final String platformId;
    private final String operation;

    public PlatformProviderOperationException(String platformId, String operation, Throwable cause) {
        super("%s provider operation failed while %s: %s".formatted(platformId, operation, cause.getMessage()), cause);
        this.platformId = platformId;
        this.operation = operation;
    }

    public String getPlatformId() {
        return platformId;
    }

    public String getOperation() {
        return operation;
    }
}
