package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel(value = "ACL Entry", description = "Defines a permission and the access type for the permission.")
public class AclEntry {

    @ApiModelProperty("UUID of a Permission.")
    public UUID permissionId;

    @ApiModelProperty(value = "Access level for the Permission.",
            notes = "Access levels for the Permission represented as bit flags. First bit is read access, " +
                    "second bit is write access, and third bit is execute access. E.g. value of 3 implies R|W access, " +
                    "5 implies R|X, 7 is R|W|X.")
    public int access;

    @ApiModelProperty("Permission name.")
    public String permission;

    public AclEntry() { }

    public AclEntry(Permission perm, Access... access) {
        this(perm.getId(), access);
        this.setPermission(perm.getName());
    }

    public AclEntry(UUID permId, int access) {
        this.permissionId = permId;
        this.access = access;
    }

    public AclEntry(String name, UUID permId, int access) {
        this.permission = name;
        this.permissionId = permId;
        this.access = access;
    }

    public AclEntry(UUID permId, Access ... access) {
        this.permissionId = permId;
        this.access = 0;

        if (access.length == 0) {
            access = Access.values();
        }

        for (Access a: access) {
            this.access = this.access + a.value;
        }
    }
    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }

    public int getAccess() {
        return access;
    }

    public AclEntry setAccess(int access) {
        this.access = access;
        return this;
    }

    public String getPermission() {
        return permission;
    }

    public AclEntry setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("perm", permissionId)
                .add("access", access)
                .add("name", permission)
                .toString();
    }
}
