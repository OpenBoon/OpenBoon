package com.zorroa.common.domain;

import java.util.List;

/**
 * Created by chambers on 6/30/16.
 */
public class PagedList<T> {

    private List<T> list;
    private Paging page;

    public PagedList() {}

    public PagedList(Paging page, List<T> list) {
        this.page = page;
        this.list = list;
    }

    public List<T> getList() {
        return list;
    }

    public PagedList setList(List<T> list) {
        this.list = list;
        return this;
    }

    public Paging getPage() {
        return page;
    }

    public PagedList setPage(Paging page) {
        this.page = page;
        return this;
    }
}
