package io.myforevermusic.api.modules.ems.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ems.flo-special")
public class FloSpecialProperties {

    private boolean enabled = true;
    private int displayLimit = 120;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDisplayLimit() {
        return displayLimit;
    }

    public void setDisplayLimit(int displayLimit) {
        this.displayLimit = displayLimit;
    }
}
