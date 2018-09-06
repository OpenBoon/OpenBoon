package com.zorroa.common.domain;

public enum Access {
    Read(1),
    Write(2),
    Export(4);

    public final int value;

    Access(int value) {
        this.value = value;
    }
}
