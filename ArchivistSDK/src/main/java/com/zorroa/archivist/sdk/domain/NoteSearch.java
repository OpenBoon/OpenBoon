package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 3/17/16.
 */
public class NoteSearch {

    private String query;
    private int page = 1;
    private int size = 20;

    public String getQuery() {
        return query;
    }

    public NoteSearch setQuery(String query) {
        this.query = query;
        return this;
    }

    public int getPage() {
        return page;
    }

    public NoteSearch setPage(int page) {
        this.page = page;
        return this;
    }

    public int getSize() {
        return size;
    }

    public NoteSearch setSize(int size) {
        this.size = size;
        return this;
    }
}
