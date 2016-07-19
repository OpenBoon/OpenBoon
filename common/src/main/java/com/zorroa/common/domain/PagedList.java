package com.zorroa.common.domain;

import com.google.common.collect.ForwardingList;

import java.util.List;

/**
 * Created by chambers on 6/30/16.
 */
public class PagedList<T> extends ForwardingList<T> {

    private List<T> list;
    private Paging page;

    public PagedList() {}

    @Override
    protected List<T> delegate() {
        return list;
    }

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
