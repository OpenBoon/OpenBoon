package com.zorroa.archivist.sdk.schema;

/**
 * Created by chambers on 1/2/16.
 */
public class VideoSchema extends MapSchema {

    private int width;
    private int height;
    private int audioSampleRate;
    private int sampleRate;
    private double duration;
    private double aspectRatio;
    private int frames;
    private int audioChannels;
    private String format;
    private double frameRate;

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


    public double getAspectRatio() {
        return aspectRatio;
    }

    public VideoSchema setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
        return this;
    }

    public int getFrames() {
        return frames;
    }

    public VideoSchema setFrames(int frames) {
        this.frames = frames;
        return this;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public VideoSchema setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public VideoSchema setFormat(String format) {
        this.format = format;
        return this;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public VideoSchema setFrameRate(double frameRate) {
        this.frameRate = frameRate;
        return this;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public VideoSchema setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }
}
