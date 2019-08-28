package com.zorroa.archivist.domain;

public enum Access {
    Read(1, "read"),
    Write(2,  "write"),
    Export(4, "export");

    public final int value;
    public final String field;

    Access(int value, String field) {
        this.value = value;
        this.field = field;
    }
}
