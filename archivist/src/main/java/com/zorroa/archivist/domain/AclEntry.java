package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.UUID;

/**
 * An AclEntry define a permission and the access type for the permission.
 */
public class AclEntry {

    public UUID permissionId;
    public int access;
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
