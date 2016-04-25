package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;

import java.util.List;
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
     * Any specific tags attached to the note.
     */
    private Set<String> tags;

    /**
     * Annotations attached to the note.
     */
    private List<Annotation> annotations;

    /**
    * The asset the note should be assign to.  Note that, an instance of the note is made PER ASSET.  This allows
    * the notes to be edited as a group or individually.
    */
    private  String asset;

    public NoteBuilder() {

    }

    public String getText() {
        return text;
    }

    public NoteBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public String getAsset() {
        return asset;
    }

    public NoteBuilder setAsset(String asset) {
        this.asset = asset;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public NoteBuilder setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public NoteBuilder setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
        return this;
    }

    public NoteBuilder addToAnnotations(Annotation annotation) {
        if (this.annotations == null) {
            this.annotations = Lists.newArrayList();
        }
        this.annotations.add(annotation);
        return this;
    }
}
