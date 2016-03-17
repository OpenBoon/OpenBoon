package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.Set;

/**
 * Created by chambers on 3/16/16.
 */
public class Note {

    private String id;
    private String text;
    private Set<String> assets;
    private Set<String> tags;
    private String author;
    private String email;

    public String getId() {
        return id;
    }

    public Note setId(String id) {
        this.id = id;
        return this;
    }

    public String getText() {
        return text;
    }

    public Note setText(String text) {
        this.text = text;
        return this;
    }

    public Set<String> getAssets() {
        return assets;
    }

    public Note setAssets(Set<String> assets) {
        this.assets = assets;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Note setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Note setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Note setEmail(String email) {
        this.email = email;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("text", text)
                .add("assets", assets)
                .add("tags", tags)
                .add("author", author)
                .add("email", email)
                .toString();
    }
}
