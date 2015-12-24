package com.zorroa.archivist.sdk.domain;

/**
 * The Access enum defines permissions and their associated value for
 * bitwise comparisons.
 */
public enum Access {

    Read(1),
    Write(2);

    private final int value;
    Access(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
