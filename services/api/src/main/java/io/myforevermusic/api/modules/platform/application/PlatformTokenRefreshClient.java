package io.myforevermusic.api.modules.platform.application;

public interface PlatformTokenRefreshClient {

    boolean supports(PlatformAccountCredential credential);

    PlatformTokenExchangeResult refreshAccessToken(PlatformAccountCredential credential);
}
