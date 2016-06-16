package com.zorroa.common.domain;

/**
 * Created by chambers on 6/16/16.
 */
public class Paging {

    private final int page;
    private final int count;

    private static final int DEFAULT_COUNT = 10;

    public static Paging first() {
        return new Paging(1, DEFAULT_COUNT);
    }

    public Paging() {
        this.page = 1;
        this.count = DEFAULT_COUNT;
    }
    public Paging (Integer page) {
        this.page = page == null ? 1 : page;
        this.count = DEFAULT_COUNT;
    }

    public Paging (Integer page, Integer count) {
        this.page = page == null ? 1 : page;
        this.count = count == null ? DEFAULT_COUNT : count;
    }
    public int getCount() {
        return count;
    }

    public int getPage() {
        return page;
    }

    public int getFrom() {
        return (page -1) * count;
    }
}
