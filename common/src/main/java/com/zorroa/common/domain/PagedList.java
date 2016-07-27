package com.zorroa.common.domain;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by chambers on 6/30/16.
 */
public class PagedList<T> implements Iterable<T> {

    private List<T> list;
    private Paging page;

    public PagedList() {
        list = Lists.newArrayList();
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

    public int size() {
        return list.size();
    }


    public T get(int idx) {
        return list.get(idx);
    }

    public Stream<T> stream() {
        return list.stream();
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }



}
