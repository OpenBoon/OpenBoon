package com.zorroa.archivist.domain;

import com.google.common.collect.Maps;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * The settings for creating a single aggregation level.
 */
public class DyHierarchyLevel {

    private static final Logger logger = LoggerFactory.getLogger(DyHierarchyLevel.class);

    /**
     * The field to be used in an aggregation.
     */
    @NotEmpty
    private String field;

    private Acl acl;

    /**
     * The type of aggregation.
     */
    @NotNull
    private DyHierarchyLevelType type = DyHierarchyLevelType.Attr;

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

    public Acl getAcl() {
        return acl;
    }

    public DyHierarchyLevel setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }

    public Object getOption(String name) {
        return this.options.get(name);
    }
}
