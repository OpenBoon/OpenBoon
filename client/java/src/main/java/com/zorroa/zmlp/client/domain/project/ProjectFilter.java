package com.zorroa.zmlp.client.domain.project;

import com.zorroa.zmlp.client.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public ProjectFilter() {
        ids = new ArrayList();
        names = new ArrayList();
        sort = new ArrayList();
    }

    public ProjectFilter withIds(List<UUID> ids){
        this.ids = ids;
        return this;
    }

    public ProjectFilter withNames(List<String> names){
        this.names = names;
        return this;
    }

    public ProjectFilter withPage(Page page){
        this.page = page;
        return this;
    }

    public ProjectFilter withSort(List<String> sort){
        this.sort = sort;
        return this;
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
