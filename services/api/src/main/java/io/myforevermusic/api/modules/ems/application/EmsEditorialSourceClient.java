package io.myforevermusic.api.modules.ems.application;

import java.util.List;

public interface EmsEditorialSourceClient {
    List<EmsEditorialArticle> fetch(EmsEditorialSource source, int limit);
}
