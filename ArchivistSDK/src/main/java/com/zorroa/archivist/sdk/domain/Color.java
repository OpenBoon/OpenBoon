package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by chambers on 4/8/16.
 */
public class Color {

    private String name;
    private String hex;
    private int red;
    private int green;
    private int blue;

    private float hue;
    private float saturation;
    private float lightness;
    private Float coverage;

    public String getName() {
        return name;
    }

    public Color setName(String name) {
        this.name = name;
        return this;
    }

    public int getRed() {
        return red;
    }

    public Color setRed(int red) {
        this.red = red;
        return this;
    }

    public int getGreen() {
        return green;
    }

    public Color setGreen(int green) {
        this.green = green;
        return this;
    }

    public int getBlue() {
        return blue;
    }

    public Color setBlue(int blue) {
        this.blue = blue;
        return this;
    }

    public float getHue() {
        return hue;
    }

    public Color setHue(float hue) {
        this.hue = hue;
        return this;
    }

    public float getSaturation() {
        return saturation;
    }

    public Color setSaturation(float saturation) {
        this.saturation = saturation;
        return this;
    }

    public float getLightness() {
        return lightness;
    }

    public Color setLightness(float lightness) {
        this.lightness = lightness;
        return this;
    }

    public Float getCoverage() {
        return coverage;
    }

    public Color setCoverage(Float coverage) {
        this.coverage = coverage;
        return this;
    }

    public String getHex() {
        return hex;
    }

    public Color setHex(String hex) {
        this.hex = hex;
        return this;
    }

    //red, orange, yellow, green, blue, indigo and violet

    Map<Range<Integer>, String[]> COLOR_RANGES = ImmutableMap.<Range<Integer>, String[]>builder()
            .put(Range.closed(0, 80), new String[] {"red", "red"})
            .put(Range.closed(9, 20), new String[] {"red", "orange"})
            .put(Range.closed(21, 39), new String[] {"orange", "brown"})
            .put(Range.closed(40, 51), new String[] {"orange", "yellow"})
            .put(Range.closed(52, 60), new String[] {"yellow", "yellow"})
            .put(Range.closed(61, 80), new String[] {"yellow", "green"})
            .put(Range.closed(81, 123), new String[] {"green", "green"})
            .put(Range.closed(81, 123), new String[] {"green", "green"})
            .put(Range.closed(124, 168), new String[] {"green", "cyan"})
            .put(Range.closed(169, 200), new String[] {"cyan", "cyan"})
            .put(Range.closed(201, 219), new String[] {"cyan", "blue"})
            .put(Range.closed(220, 245), new String[] {"blue", "blue"})
            .put(Range.closed(246, 275), new String[] {"blue", "purple"})
            .put(Range.closed(276, 311), new String[] {"purple", "purple"})
            .put(Range.closed(312, 340), new String[] {"purple", "pink"})
            .put(Range.closed(341, 350), new String[] {"pink", "pink"})
            .put(Range.closed(342, 357), new String[] {"pink", "red"})
            .put(Range.closed(358, 360), new String[] {"red", "red"})
            .build();

    @JsonIgnore
    public Set<String> getKeywords() {
        Set<String>  result = Sets.newHashSet();
        if (hue == 0) {
            float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB(red, green, blue, hsb);
            this.setHue(hsb[0]);
            this.setSaturation(hsb[1]);
            this.setLightness(hsb[2]);
        }

        if (this.getSaturation() <= 10) {
            result.add("grey");
            return result;
        }

        if (this.getSaturation() <= 50) {
            result.add("grey");
        }

        for (Map.Entry<Range<Integer>, String[]> entry: COLOR_RANGES.entrySet()) {
            if (entry.getKey().contains((int) hue)) {
                result.addAll(Arrays.asList(entry.getValue()));
                break;
            }
        }

        double a = 1 - ( 0.299 * red + 0.587 * green + 0.114 * blue)/255;
        if (a <= 0.4) {
            result.add("bright");
        }
        else if (a >= 0.7) {
            result.add("dark");
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return getRed() == color.getRed() &&
                getGreen() == color.getGreen() &&
                getBlue() == color.getBlue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRed(), getGreen(), getBlue());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("red", red)
                .add("green", green)
                .add("blue", blue)
                .toString();
    }
}
