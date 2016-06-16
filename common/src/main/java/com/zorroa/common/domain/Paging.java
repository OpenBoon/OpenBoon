package com.zorroa.common.domain;

/**
 * Created by chambers on 6/16/16.
 */
public class Paging {

    private final int page;
    private final int count;

    public Paging (int page) {
        this.page = page;
        this.count = 10;
    }

    public Paging (int page, int count) {
        this.page = page;
        this.count = count;
    }
    public int getCount() {
        return count;
    }

    public int getPage() {
        return page;
    }
}
