package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * The PermissionSchema contains the permissions needed to access the asset.
 */
public class PermissionSchema {

    Set<Integer> search = Sets.newHashSet();
    Set<Integer> export = Sets.newHashSet();
    Set<Integer> write = Sets.newHashSet();

    public Set<Integer> getSearch() {
        return search;
    }
    public PermissionSchema setSearch(Set<Integer> search) {
        this.search = search;
        return this;
    }

    public Set<Integer> getExport() {
        return export;
    }

    public PermissionSchema setExport(Set<Integer> export) {
        this.export = export;
        return this;
    }

    public Set<Integer> getWrite() {
        return write;
    }

    public PermissionSchema setWrite(Set<Integer> write) {
        this.write = write;
        return this;
    }
}
