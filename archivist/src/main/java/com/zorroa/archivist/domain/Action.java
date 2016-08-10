package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

/**
 * Created by chambers on 8/9/16.
 */
public class Action {

    public enum Type {
        ADD_TO_FOLDER,
        SET_PERMISSION
    }

    private Action.Type type;
    private Integer folderId;
    private Integer permissionId;
    private String value;

    public Integer getFolderId() {
        return folderId;
    }

    public Action setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public Action setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Action setType(Type type) {
        this.type = type;
        return this;
    }

    public String getValue() {
        return value;
    }

    public Action setValue(String value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("folderId", folderId)
                .add("permissionId", permissionId)
                .toString();
    }
}
