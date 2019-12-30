package com.zorroa.zmlp.sdk.domain;

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
    private String timeline;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getStart() {
        return start;
    }

    public void setStart(Float start) {
        this.start = start;
    }

    public Float getStop() {
        return stop;
    }

    public void setStop(Float stop) {
        this.stop = stop;
    }

    public String getTimeline() {
        return timeline;
    }

    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }
}
