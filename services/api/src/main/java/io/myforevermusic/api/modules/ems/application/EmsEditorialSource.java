package io.myforevermusic.api.modules.ems.application;

public record EmsEditorialSource(
    String name,
    String type,
    String url,
    double weight
) {
}
