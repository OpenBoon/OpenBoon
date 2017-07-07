package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by chambers on 7/6/17.
 */
public class Setting {

    private String name;
    private String defaultValue;
    private String currentValue;
    private boolean live;
    private String description;
    private String category;

    public String getDescription() {
        return description;
    }

    public Setting setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public Setting setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getName() {
        return name;
    }

    public Setting setName(String name) {
        this.name = name;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Setting setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public Setting setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
        return this;
    }

    public boolean isLive() {
        return live;
    }

    public Setting setLive(boolean live) {
        this.live = live;
        return this;
    }

    public boolean isDefault() {
        if (defaultValue == null || currentValue == null) {
            return defaultValue == currentValue;
        }
        return defaultValue.equals(currentValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Setting setting = (Setting) o;
        return Objects.equals(getName(), setting.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("currentValue", currentValue)
                .toString();
    }
}
