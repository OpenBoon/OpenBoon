package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by chambers on 6/16/16.
 */
public class Pager {

    private final int from;
    private final int size;

    private Long totalCount;

    private static final int DEFAULT_SIZE = 10;

    public static Pager first() {
        return new Pager(1, DEFAULT_SIZE);
    }

    public static Pager first(int size) { return new Pager(1, size);
    }

    public Pager() {
        this.from = 0;
        this.size = DEFAULT_SIZE;
    }

    public Pager(Integer from, Integer count, Integer totalCount) {
        this.size = count == null || count <= 0 ? DEFAULT_SIZE : count;
        this.from = from == null || from < 1 ? 0 : from;
        this.totalCount = Long.valueOf(totalCount == null || totalCount < 0 ? 0 : totalCount);
    }

    // WARNING: The following two constructors are for backwards-compatibility
    //          with the old page+count initialization. These should be removed
    //          after we refactor the non-asset Pagers

    public Pager(Integer page) {
        this.size = DEFAULT_SIZE;
        this.from = page == null || page < 1 ? 0 : (Math.max(1, page) - 1) * DEFAULT_SIZE;
    }

    public Pager(Integer page, Integer count) {
        this.size = count == null || count < 0 ? DEFAULT_SIZE : Math.max(0, count);
        this.from = page == null || page < 1 ? 0 : (Math.max(1, page) - 1) * this.size;
    }

    public int getSize() {
        return size;
    }

    public int getFrom() { return from; }

    @JsonIgnore
    public int getClosestPage() {
        return from >= 0 && size > 0 ? Math.toIntExact(from / size) + 1 : 1;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Pager setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    @JsonIgnore
    public int getTotalPages() {
        return size > 0 ? Math.toIntExact(totalCount / size) + 1 : 0;
    }

    @JsonIgnore
    public int getNext() {
        return Math.min(getTotalPages(), getClosestPage() + 1);
    }

    @JsonIgnore
    public int getPrev() {
        return Math.max(1, getClosestPage() - 1);
    }

    @JsonIgnore
    public String getDisplay() {
        return getClosestPage() + " of " + getTotalPages();
    }
}

