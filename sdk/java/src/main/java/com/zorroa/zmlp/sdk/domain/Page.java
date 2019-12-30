package com.zorroa.zmlp.sdk.domain;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Used to describe results page when paginating responses.
 */
public class Page {
    /**
     * Result index to start from.
     */
    private Integer from = 0;

    /**
     * Number of results per page.
     */
    private Integer size = 25;

    private Long totalCount;

    public Page() { }

    public Page(Integer from, Integer size, Long totalCount) {
        this.from = from;
        this.size = size;
        this.totalCount = totalCount;
    }

    public Integer getFrom() {
        return from;
    }

    public Page setFrom(Integer from) {
        this.from = from;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public Page setSize(Integer size) {
        this.size = size;
        return this;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Page setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
        return this;
    }
}

