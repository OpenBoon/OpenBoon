package com.zorroa.archivist.domain;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * The settings for creating a single aggregation level.
 */
public class DyHierarchyLevel {

    /**
     * The field to be used in an aggregation.
     */
    private String field;

    /**
     * The type of aggregation.
     */
    private DyHierarchyLevelType type = DyHierarchyLevelType.Term;

    /**
     * Options which can modify the behavior of the DyHierarchy generator.
     */
    private Map<String, Object> options = Maps.newHashMap();

    public DyHierarchyLevel() {}

    public DyHierarchyLevel(String field) {
        this.field = field;
    }

    public DyHierarchyLevel(String field, DyHierarchyLevelType type) {
        this.field = field;
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public DyHierarchyLevel setField(String field) {
        this.field = field;
        return this;
    }

    public DyHierarchyLevelType getType() {
        return type;
    }

    public DyHierarchyLevel setType(DyHierarchyLevelType type) {
        this.type = type;
        return this;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public DyHierarchyLevel setOptions(Map<String, Object> options) {
        this.options = options;
        return this;
    }

    public DyHierarchyLevel setOption(String name, Object val) {
        this.options.put(name, val);
        return this;
    }
}
