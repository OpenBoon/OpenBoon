package com.zorroa.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by chambers on 6/16/16.
 */
public class Paging {

    private final int number;
    private final int size;

    private Long totalCount;
    private int totalPages;
    private int next;
    private int prev;
    private String display;

    private static final int DEFAULT_SIZE = 10;

    public static Paging first() {
        return new Paging(1, DEFAULT_SIZE);
    }

    public static Paging first(int size) { return new Paging(1, size);
    }

    public Paging() {
        this.number = 1;
        this.size = DEFAULT_SIZE;
    }
    public Paging (Integer page) {
        this.number = page == null ? 1 : page;
        this.size = DEFAULT_SIZE;
    }

    public Paging (Integer page, Integer count) {
        this.number = page == null ? 1 : page;
        this.size = count == null ? DEFAULT_SIZE : count;
    }
    public int getSize() {
        return size;
    }

    public int getNumber() {
        return number;
    }

    @JsonIgnore
    public int getFrom() {
        return (number -1) * size;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Paging setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
        totalPages = Math.toIntExact(totalCount / getSize()) +1;
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
