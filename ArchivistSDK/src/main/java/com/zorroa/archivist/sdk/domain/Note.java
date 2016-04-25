package com.zorroa.archivist.sdk.domain;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 4/8/16.
 */
public class Note {

    private String id;
    private String author;
    private String email;
    private String text;
    private String asset;
    /**
     * Any specific tags attached to the note.
     */
    private Set<String> tags;
    private Date createdTime;
    private Date modifiedTime;
    private List<Annotation> annotations;

    public String getId() {
        return id;
    }

    public Note setId(String id) {
        this.id = id;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Note setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getText() {
        return text;
    }

    public Note setText(String text) {
        this.text = text;
        return this;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Note setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public Note setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Note setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getAsset() {
        return asset;
    }

    public Note setAsset(String asset) {
        this.asset = asset;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Note setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public Note setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
        return this;
    }
}
