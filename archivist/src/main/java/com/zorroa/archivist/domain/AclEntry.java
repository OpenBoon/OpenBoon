package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

/**
 * An AclEntry define a permission and the access type for the permission.
 */
public class AclEntry {

    public int permissionId;
    public int access;

    public AclEntry() { }

    public AclEntry(Permission perm, Access... access) {
        this(perm.getId(), access);
    }

    public AclEntry(int permId, int access) {
        this.permissionId = permId;
        this.access = access;
    }

    public AclEntry(int permId, Access... access) {
        this.permissionId = permId;
        this.access = 0;

        if (access.length == 0) {
            access = Access.values();
        }

        for (Access a: access) {
            this.access = this.access + a.getValue();
        }
    }
    public int getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(int permissionId) {
        this.permissionId = permissionId;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("perm", permissionId)
                .add("access", access)
                .toString();
    }
}
