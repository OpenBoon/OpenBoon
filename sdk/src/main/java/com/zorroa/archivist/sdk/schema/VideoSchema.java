package com.zorroa.archivist.sdk.schema;

import com.google.common.base.MoreObjects;

/**
 * Created by chambers on 1/2/16.
 */
public class VideoSchema extends ExtendableSchema<String, Object> {

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
    private String synopsis;
    private String description;
    private String title;

    public String getSynopsis() {
        return synopsis;
    }

    public VideoSchema setSynopsis(String synopsis) {
        this.synopsis = synopsis;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public VideoSchema setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public VideoSchema setTitle(String title) {
        this.title = title;
        return this;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("width", width)
                .add("height", height)
                .add("audioSampleRate", audioSampleRate)
                .add("sampleRate", sampleRate)
                .add("duration", duration)
                .add("aspectRatio", aspectRatio)
                .add("frames", frames)
                .add("audioChannels", audioChannels)
                .add("format", format)
                .add("frameRate", frameRate)
                .add("synopsis", synopsis)
                .add("description", description)
                .add("title", title)
                .toString();
    }
}
