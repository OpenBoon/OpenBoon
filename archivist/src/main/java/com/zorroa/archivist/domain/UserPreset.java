package com.zorroa.archivist.domain;

import java.util.UUID;

/**
 * Created by chambers on 10/17/16.
 */
public class UserPreset extends UserPresetSpec {

    private UUID presetId;

    public UUID getPresetId() {
        return presetId;
    }

    public UserPreset setPresetId(UUID presetId) {
        this.presetId = presetId;
        return this;
    }
}
