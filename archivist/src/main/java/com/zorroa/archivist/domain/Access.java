package com.zorroa.archivist.domain;

public enum Access {
    Read(1, "read"),
    Write(2,  "write"),
    Export(4, "export"),
    Delete(8, "delete");

    public static final int MAX = 15;

    public final int value;
    public final String field;

    Access(int value, String field) {
        this.value = value;
        this.field = field;
    }
}
