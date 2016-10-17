package com.zorroa.archivist.domain;

/**
 * Created by chambers on 10/17/16.
 */
public class UserPreset extends UserPresetSpec {

    private int presetId;

    public int getPresetId() {
        return presetId;
    }

    public UserPreset setPresetId(int presetId) {
        this.presetId = presetId;
        return this;
    }
}
