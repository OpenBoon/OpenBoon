package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Specification for a Preset object, which contains a predefined set of user
 * defaults.
 */
public class UserPresetSpec {

    /**
     * The name of the preset.
     */
    @NotEmpty
    private String name;

    /**
     * The permissions this preset will assign.
     */
    private List<Integer> permissionIds;

    /**
     * The user settings this preset will assign.
     */
    private UserSettings settings;

    public List<Integer> getPermissionIds() {
        return permissionIds;
    }

    public UserPresetSpec setPermissionIds(List<Integer> permissionIds) {
        this.permissionIds = permissionIds;
        return this;
    }

    public UserSettings getSettings() {
        return settings;
    }

    public UserPresetSpec setSettings(UserSettings settings) {
        this.settings = settings;
        return this;
    }

    public String getName() {
        return name;
    }

    public UserPresetSpec setName(String name) {
        this.name = name;
        return this;
    }
}

