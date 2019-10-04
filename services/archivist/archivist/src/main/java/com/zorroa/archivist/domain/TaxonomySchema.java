package com.zorroa.archivist.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TaxonomySchema {

    private List<String> keywords;
    private List<String> suggest;
    private long updatedTime;
    private UUID folderId;
    private UUID taxId;

    public List<String> getKeywords() {
        return keywords;
    }

    public TaxonomySchema setKeywords(List<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public List<String> getSuggest() {
        return suggest;
    }

    public TaxonomySchema setSuggest(List<String> suggest) {
        this.suggest = suggest;
        return this;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public TaxonomySchema setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public TaxonomySchema setFolderId(UUID folderId) {
        this.folderId = folderId;
        return this;
    }

    public UUID getTaxId() {
        return taxId;
    }

    public TaxonomySchema setTaxId(UUID taxId) {
        this.taxId = taxId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxonomySchema that = (TaxonomySchema) o;
        return getTaxId() == that.getTaxId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaxId());
    }
}
