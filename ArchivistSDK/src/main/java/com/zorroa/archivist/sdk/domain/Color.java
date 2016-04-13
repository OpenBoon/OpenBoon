package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chambers on 4/8/16.
 */
public class Color {

    private static final Logger logger = LoggerFactory.getLogger(Color.class);

    private String name;
    private String hex;
    private int red;
    private int green;
    private int blue;

    private Float hue;
    private Float saturation;
    private Float lightness;
    private Float coverage;

    public Color() {}

    public Color(int r, int g, int b) {
        this.red = r;
        this.green = g;
        this.blue = b;
        this.calculateHSV();
    }
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

    public Float getHue() {
        return hue;
    }

    public Color setHue(Float hue) {
        this.hue = hue;
        return this;
    }

    public Float getSaturation() {
        return saturation;
    }

    public Color setSaturation(Float saturation) {
        this.saturation = saturation;
        return this;
    }

    public Float getLightness() {
        return lightness;
    }

    public Color setLightness(Float lightness) {
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

    private static final Map<Range<Integer>, String[]> COLOR_RANGES = ImmutableMap.<Range<Integer>, String[]>builder()
            .put(Range.closed(0, 80), new String[] {"red", "red"})
            .put(Range.closed(9, 20), new String[] {"red", "orange"})
            .put(Range.closed(21, 39), new String[] {"orange", "brown"})
            .put(Range.closed(40, 51), new String[] {"orange", "yellow"})
            .put(Range.closed(52, 60), new String[] {"yellow", "yellow"})
            .put(Range.closed(61, 80), new String[] {"yellow", "green"})
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
    public String getKeywords() {
        List<String> result = Lists.newArrayList();
        if (hue == null) {
            calculateHSV();
        }

        if (this.getLightness() > 98) {
            return "white";
        }

        if (this.getLightness() <= 10) {
            return "black";
        }

        double a = 1 - ( 0.299 * red + 0.587 * green + 0.114 * blue)/255.0;
        if (a <= 0.4) {
            result.add("bright");
        }
        else if (a >= 0.7) {
            result.add("dark");
        }

        if (this.getSaturation() <= 10) {
            result.add("grey");
        }

        for (Map.Entry<Range<Integer>, String[]> entry: COLOR_RANGES.entrySet()) {
            if (entry.getKey().contains(hue.intValue())) {
                result.addAll(Arrays.asList(entry.getValue()));
                break;
            }
        }
        return String.join(" ", result);
    }

    private void calculateHSV() {
        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;

        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float h, s, v;
        s = v = (max + min) / 2.0f;

        float d = max - min;

        if (max == min) {
            h = 0; // achromatic
        } else {
            s = v > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h *= 6;
        }

        this.hue = h * 10;
        this.saturation = s * 100;
        this.lightness = v * 100;
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
