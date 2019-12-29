package com.zorroa.zmlp.sdk;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Used to describe results page when paginating responses.
 */
public class KPage {
    /**
     * Result index to start from.
     */
    private Integer from;

    /**
     * Number of results per page.
     */
    private Integer size;

    private Long totalCount;

    public KPage(Integer from, Integer size, Long totalCount) {
        this.from = from;
        this.size = size;
        this.totalCount = totalCount;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Map toMap() {
        Map map = new HashMap();

        Optional.ofNullable(from).ifPresent((value) -> map.put("from", value));
        Optional.ofNullable(size).ifPresent((value) -> map.put("size", value));
        Optional.ofNullable(totalCount).ifPresent((value) -> map.put("totalCount", value));

        return map;
    }
}

