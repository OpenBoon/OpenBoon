package com.zorroa.archivist.domain;

public enum Access {
    Read(1),
    Write(2),
    Export(4);

    public final int value;

    Access(int value) {
        this.value = value;
    }
}
