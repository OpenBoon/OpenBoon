package com.zorroa.archivist.sdk.schema;

import com.zorroa.archivist.sdk.util.Json;

/**
 * Created by chambers on 1/3/16.
 */
public class DocumentSchema implements Schema {

    @Keyword
    private String title;
    @Keyword
    private String author;

    private String body;

    private int pages;

    public String getTitle() {
        return title;
    }

    public DocumentSchema setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public DocumentSchema setAuthor(String author) {
        this.author = author;
        return this;
    }

    public int getPages() {
        return pages;
    }

    public DocumentSchema setPages(int pages) {
        this.pages = pages;
        return this;
    }

    public String getBody() {
        return body;
    }

    public DocumentSchema setBody(String body) {
        this.body = body;
        return this;
    }

    @Override
    public String getNamespace() {
        return "document";
    }

    public String toString() {
        return Json.serializeToString(this);
    }
}
