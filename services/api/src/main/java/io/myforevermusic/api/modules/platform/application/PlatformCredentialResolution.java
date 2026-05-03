package io.myforevermusic.api.modules.platform.application;

import java.util.Optional;

public record PlatformCredentialResolution(
    String status,
    PlatformAccountCredential credential,
    String detail
) {

    public static final String STATUS_READY = "ready";
    public static final String STATUS_MISSING = "missing";
    public static final String STATUS_RECONNECT_REQUIRED = "reconnect_required";

    public static PlatformCredentialResolution ready(PlatformAccountCredential credential) {
        return new PlatformCredentialResolution(STATUS_READY, credential, null);
    }

    public static PlatformCredentialResolution missing() {
        return new PlatformCredentialResolution(STATUS_MISSING, null, null);
    }

    public static PlatformCredentialResolution reconnectRequired(String detail) {
        return new PlatformCredentialResolution(STATUS_RECONNECT_REQUIRED, null, detail);
    }

    public boolean usable() {
        return STATUS_READY.equals(status) && credential != null;
    }

    public boolean reconnectRequired() {
        return STATUS_RECONNECT_REQUIRED.equals(status);
    }

    public boolean needsReconnect(boolean connected) {
        return connected && !usable();
    }

    public Optional<PlatformAccountCredential> usableCredential() {
        return usable() ? Optional.of(credential) : Optional.empty();
    }
}
