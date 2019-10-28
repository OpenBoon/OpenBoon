package com.zorroa.archivist.domain;

import com.zorroa.archivist.search.Scroll;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by chambers on 6/30/16.
 */
@ApiModel(value = "Paged List", description = "Paginated list of items.")
public class PagedList<T> implements Iterable<T> {

    @ApiModelProperty("List of items to paginate.")
    private List<T> list;

    @ApiModelProperty("Current page.")
    private Pager page;

    @ApiModelProperty("Aggregations to apply to list.")
    private Map<String, Object> aggregations;

    @ApiModelProperty("Scroll used to get the next page.")
    private Scroll scroll;

    public PagedList() {
        list = new ArrayList();
    }

    public PagedList(Pager page, List<T> list) {
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

    public Pager getPage() {
        return page;
    }

    public PagedList setPage(Pager page) {
        this.page = page;
        return this;
    }

    public Map<String, Object> getAggregations() {
        return aggregations;
    }

    public PagedList setAggregations(Map<String, Object> aggregations) {
        this.aggregations = (Map<String, Object>) aggregations.get("aggregations");
        return this;
    }

    public PagedList setScroll(Scroll scroll) {
        this.scroll = scroll;
        return this;
    }

    public Scroll getScroll() {
        return scroll;
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

