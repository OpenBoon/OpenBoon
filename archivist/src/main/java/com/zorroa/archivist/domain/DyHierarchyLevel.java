package com.zorroa.archivist.domain;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Map;

@ApiModel(value = "Dynamic Hierarchy Level", description = "Settings for creating a single aggregation level")
public class DyHierarchyLevel {

    private static final Logger logger = LoggerFactory.getLogger(DyHierarchyLevel.class);

    @NotEmpty
    @ApiModelProperty("Field to be used in an aggregation.")
    private String field;

    @ApiModelProperty("ACL to apply to the Level.")
    private Acl acl;

    @NotNull
    @ApiModelProperty("Type of aggregation.")
    private DyHierarchyLevelType type = DyHierarchyLevelType.Attr;

    @ApiModelProperty("Options which can modify the behavior of the DyHierarchy generator.")
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
