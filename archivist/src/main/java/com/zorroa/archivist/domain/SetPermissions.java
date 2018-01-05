package com.zorroa.archivist.domain;

public class SetPermissions {

    public Acl acl;
    public boolean replace = false;

    public Acl getAcl() {
        return acl;
    }

    public SetPermissions setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }

    public boolean isReplace() {
        return replace;
    }

    public SetPermissions setReplace(boolean replace) {
        this.replace = replace;
        return this;
    }
}
