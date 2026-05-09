package io.myforevermusic.api.modules.platform.application;

public interface PlatformAccountProfileResolver {

    boolean supports(String platformId);

    PlatformAccountProfile resolve(PlatformAccountCredential credential);
}
