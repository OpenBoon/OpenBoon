package com.zorroa.zmlp.client.domain;

/**
 * Defines a subsection of an Asset that was processed,
 * for example a page of a document.
 */
public class Clip {

    /**
     * The clip type, usually 'scene' or 'page' but it can be arbitrary.
     */
    private String type;

    /**
     * The start of the clip
     */
    private Float start;

    /**
     * The end of the clip
     */
    private Float stop;

    /**
     * Used when multiple type of clipification on a video occur
     */
    private String track;

    public String getType() {
        return type;
    }

    public Clip setType(String type) {
        this.type = type;
        return this;
    }

    public Float getStart() {
        return start;
    }

    public Clip setStart(Float start) {
        this.start = start;
        return this;
    }

    public Float getStop() {
        return stop;
    }

    public Clip setStop(Float stop) {
        this.stop = stop;
        return this;
    }

    public String getTrack() {
        return track;
    }

    public Clip setTrack(String track) {
        this.track = track;
        return this;
    }
}
