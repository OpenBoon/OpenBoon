package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

/**
 * Created by chambers on 8/9/16.
 */
public class ActionSpec {

    private Action.Type type;
    private Integer folderId;
    private Integer permissionId;

    private String strValue;
    private Integer intValue;
    private Float floatValue;

    public Integer getFolderId() {
        return folderId;
    }

    public ActionSpec setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public ActionSpec setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public Action.Type getType() {
        return type;
    }

    public ActionSpec setType(Action.Type type) {
        this.type = type;
        return this;
    }

    public String getStrValue() {
        return strValue;
    }

    public ActionSpec setStrValue(String strValue) {
        this.strValue = strValue;
        return this;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public ActionSpec setIntValue(Integer intValue) {
        this.intValue = intValue;
        return this;
    }

    public Float getFloatValue() {
        return floatValue;
    }

    public ActionSpec setFloatValue(Float floatValue) {
        this.floatValue = floatValue;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("folderId", folderId)
                .add("permissionId", permissionId)
                .add("strValue", strValue)
                .add("intValue", intValue)
                .add("floatValue", floatValue)
                .toString();
    }
}
