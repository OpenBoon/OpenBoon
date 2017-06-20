package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by chambers on 6/17/17.
 */
public class Taxonomy {

    private int taxonomyId;
    private int folderId;

    public int getTaxonomyId() {
        return taxonomyId;
    }

    public Taxonomy setTaxonomyId(int taxonomyId) {
        this.taxonomyId = taxonomyId;
        return this;
    }

    public int getFolderId() {
        return folderId;
    }

    public Taxonomy setFolderId(int folderId) {
        this.folderId = folderId;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("taxonomyId", taxonomyId)
                .add("folderId", folderId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Taxonomy taxonomy = (Taxonomy) o;
        return getTaxonomyId() == taxonomy.getTaxonomyId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaxonomyId());
    }
}
