package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * An Access Control List is a list of AclEntry objects.
 */
public class Acl extends ForwardingList<AclEntry> {

    List<AclEntry> delegate;

    public Acl() {
        this.delegate = Lists.newArrayList();
    }

    public Acl addAll(Acl acl) {
        for (AclEntry entry: acl) {
            delegate.add(entry);
        }
        return this;
    }

    public Acl addEntry(Permission perm, Access... access) {
        this.delegate.add(new AclEntry(perm, access));
        return this;
    }

    public Acl addEntry(int perm, Access... access) {
        this.delegate.add(new AclEntry(perm, access));
        return this;
    }

    public boolean hasAccess(Permission perm, Access access) {
        return hasAccess(perm.getId(), access);
    }

    public boolean hasAccess(int perm, Access access) {
        if (delegate.isEmpty()) {
            return true;
        }
        for (AclEntry entry: delegate) {
            if (entry.getPermissionId() == perm &&
                    (access.getValue() & entry.getAccess()) == access.getValue()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAccess(Iterable<Integer> permissions, Access access) {
        if (delegate.isEmpty()) {
            return true;
        }
        for (int perm: permissions) {
            if (hasAccess(perm, access)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @JsonIgnore
    protected List<AclEntry> delegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("entries", delegate).toString();
    }
}
