package com.zorroa.archivist.web.gui.forms;

/**
 * Created by chambers on 7/31/16.
 */
public class UserStateForm {

    private String enabled;

    public boolean isEnabled() {
        return Boolean.valueOf(enabled);
    }

    public UserStateForm setEnabled(String enabled) {
        this.enabled = enabled;
        return this;
    }
}
