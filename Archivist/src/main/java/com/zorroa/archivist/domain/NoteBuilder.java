package com.zorroa.archivist.domain;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by chambers on 3/16/16.
 */
public class NoteBuilder {

    /**
     * The text of the note.
     */
    private String text;

    /**
     * The asset the note should be assign to.  Note that, an instance of the note is made PER ASSET.  This allows
     * the notes to be edited as a group or individually.
     */
    private  Set<String> assets;

    /**
     * Tags in the note.  Additional tags get parsed out of the text using #<tag>
     */
    private Set<String> tags;

    public NoteBuilder() {
        assets = Sets.newHashSet();
        tags = Sets.newHashSet();
    }

    public String getText() {
        return text;
    }

    public NoteBuilder setText(String text) {
        this.text = text;
        return this;
    }


    public Set<String> getTags() {
        return tags;
    }

    public NoteBuilder setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }


    public Set<String> getAssets() {
        return assets;
    }

    public NoteBuilder setAssets(Set<String> assets) {
        this.assets = assets;
        return this;
    }
}
