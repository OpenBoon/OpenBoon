package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by chambers on 3/16/16.
 */
public class NoteSpec {

    /**
     * The text of the note.
     */
    @NotEmpty
    private String text;

    /**
    * The asset the note should be assign to.  Note that, an instance of the note is made PER ASSET.  This allows
    * the notes to be edited as a group or individually.
    */
    @NotEmpty
    private String asset;

    public NoteSpec() { }

    public String getText() {
        return text;
    }

    public NoteSpec setText(String text) {
        this.text = text;
        return this;
    }

    public String getAsset() {
        return asset;
    }

    public NoteSpec setAsset(String asset) {
        this.asset = asset;
        return this;
    }
}
