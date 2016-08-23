package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by chambers on 8/9/16.
 */
public class MatcherSpec {

    @NotEmpty
    private String attr;

    @NotEmpty
    private String op;

    private String value;

    public String getAttr() {
        return attr;
    }

    public MatcherSpec setAttr(String attr) {
        this.attr = attr;
        return this;
    }

    public String getValue() {
        return value;
    }

    public MatcherSpec setValue(String value) {
        this.value = value;
        return this;
    }

    public String getOp() {
        return op;
    }

    public MatcherSpec setOp(String op) {
        this.op = op;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("attr", attr)
                .add("op", op)
                .add("value", value)
                .toString();
    }
}
