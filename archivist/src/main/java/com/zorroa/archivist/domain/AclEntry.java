package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

/**
 * An AclEntry define a permission and the access type for the permission.
 */
public class AclEntry {

    public Integer permissionId;
    public int access;
    public String permission;
    public AclEntry() { }

    public AclEntry(Permission perm, Access... access) {
        this(perm.getId(), access);
        this.setPermission(perm.getName());
    }

    public AclEntry(int permId, int access) {
        this.permissionId = permId;
        this.access = access;
    }

    public AclEntry(String name, int permId, int access) {
        this.permission = name;
        this.permissionId = permId;
        this.access = access;
    }

    public AclEntry(int permId, Access ... access) {
        this.permissionId = permId;
        this.access = 0;

        if (access.length == 0) {
            access = Access.values();
        }

        for (Access a: access) {
            this.access = this.access + a.getValue();
        }
    }
    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
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
                .toString();
    }
}
