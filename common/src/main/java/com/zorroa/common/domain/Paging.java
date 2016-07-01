package com.zorroa.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by chambers on 6/16/16.
 */
public class Paging {

    private final int number;
    private final int count;

    private Long totalCount;
    private int totalPages;
    private int next;
    private int prev;
    private String display;

    private static final int DEFAULT_COUNT = 10;

    public static Paging first() {
        return new Paging(1, DEFAULT_COUNT);
    }

    public Paging() {
        this.number = 1;
        this.count = DEFAULT_COUNT;
    }
    public Paging (Integer page) {
        this.number = page == null ? 1 : page;
        this.count = DEFAULT_COUNT;
    }

    public Paging (Integer page, Integer count) {
        this.number = page == null ? 1 : page;
        this.count = count == null ? DEFAULT_COUNT : count;
    }
    public int getCount() {
        return count;
    }

    public int getNumber() {
        return number;
    }

    @JsonIgnore
    public int getFrom() {
        return (number -1) * count;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Paging setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
        totalPages = Math.toIntExact(totalCount / getCount()) +1;
        prev = Math.max(1, number-1);
        next = Math.min(totalPages, number+1);
        display = String.format("%d of %d", number, totalPages);
        return this;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getNext() {
        return next;
    }

    public int getPrev() {
        return prev;
    }

    public String getDisplay() {
        return display;
    }
}
