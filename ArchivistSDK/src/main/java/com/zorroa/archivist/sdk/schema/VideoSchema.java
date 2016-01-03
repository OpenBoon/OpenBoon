package com.zorroa.archivist.sdk.schema;

/**
 * Created by chambers on 1/2/16.
 */
public class VideoSchema implements Schema {

    private int width;
    private int height;
    private int audioSampleRate;
    private double duration;

    @Override
    public String getNamespace() {
        return "video";
    }

    public int getWidth() {
        return width;
    }

    public VideoSchema setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public VideoSchema setHeight(int height) {
        this.height = height;
        return this;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public VideoSchema setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
        return this;
    }

    public double getDuration() {
        return duration;
    }

    public VideoSchema setDuration(double duration) {
        this.duration = duration;
        return this;
    }
}
