package com.zorroa.zmlp.sdk.domain;

import java.util.*;

/**
 * Search filter for finding Projects
 */
public class ProjectFilter {
    /**
     * The Project IDs to match.
     */
    private List<UUID> ids;

    /**
     * The project names to match
     */
    private List<String> names;

    private Page page;

    private List<String> sort;

    public ProjectFilter(List<UUID> ids, List<String> names, Page page, List<String> sort) {
        this.ids = ids;
        this.names = names;
        this.page = page;
        this.sort = sort;
    }

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> ids) {
        this.ids = ids;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public List<String> getSort() {
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
    }
}
