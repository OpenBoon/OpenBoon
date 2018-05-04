package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;
import com.zorroa.sdk.domain.Access;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        this.delegate.add(new AclEntry(perm.getId(), access));
        return this;
    }

    public Acl addEntry(Permission perm, int access) {
        this.delegate.add(new AclEntry(perm.getId(), access));
        return this;
    }

    public Acl addEntry(UUID perm, int access) {
        this.delegate.add(new AclEntry(perm, access));
        return this;
    }

    public Acl addEntry(UUID perm, String name, int access) {
        this.delegate.add(new AclEntry(perm, access).setPermission(name));
        return this;
    }

    public Acl addEntry(UUID perm, Access access) {
        this.delegate.add(new AclEntry(perm, access));
        return this;
    }

    public Acl addEntry(String name, int access) {
        this.delegate.add(new AclEntry().setPermission(name).setAccess(access));
        return this;
    }

    public boolean hasAccess(Permission perm, Access access) {
        return hasAccess(perm.getId(), access);
    }

    public boolean hasAccess(UUID perm, Access access) {
        if (delegate.isEmpty()) {
            return true;
        }
        for (AclEntry entry: delegate) {
            if (Objects.equals(entry.getPermissionId(), perm) &&
                    (access.value & entry.getAccess()) == access.value) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAccess(Iterable<UUID> permissions, Access access) {
        if (delegate.isEmpty()) {
            return true;
        }
        for (UUID perm: permissions) {
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
