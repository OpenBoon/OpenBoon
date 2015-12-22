package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * The PermissionSchema contains the permissions needed to access the asset.
 */
public class PermissionSchema implements Schema {

    Set<Integer> search = Sets.newHashSet();
    Set<Integer> export = Sets.newHashSet();

    @Override
    public String getNamespace() {
        return "permissions";
    }

    public Set<Integer> getSearch() {
        return search;
    }

    public void setSearch(Set<Integer> search) {
        this.search = search;
    }

    public Set<Integer> getExport() {
        return export;
    }

    public void setExport(Set<Integer> export) {
        this.export = export;
    }
}
