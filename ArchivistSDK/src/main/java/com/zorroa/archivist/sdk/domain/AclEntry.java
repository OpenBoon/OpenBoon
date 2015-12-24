package com.zorroa.archivist.sdk.domain;

/**
 * An AclEntry define a permission and the access type for the permission.
 */
public class AclEntry {

    public int permissionId;
    public int access;

    public AclEntry() { }

    public AclEntry(Permission perm, Access... access) {
        this.permissionId = perm.getId();
        this.access = 0;
        for (Access a: access) {
            this.access = this.access + a.getValue();
        }
    }
    public AclEntry(int permId, Access... access) {
        this.permissionId = permId;
        this.access = 0;
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


}
