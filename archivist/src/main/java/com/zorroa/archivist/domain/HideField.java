package com.zorroa.archivist.domain;

public class HideField {

    private String field;
    private boolean hide;
    private boolean manual = false;

    public HideField() { }

    public HideField(String field, boolean hide) {
        this.field = field;
        this.hide = hide;
    }

    public boolean isHide() {
        return hide;
    }

    public HideField setHide(boolean hide) {
        this.hide = hide;
        return this;
    }

    public boolean isManual() {
        return manual;
    }

    public HideField setManual(boolean manual) {
        this.manual = manual;
        return this;
    }

    public String getField() {
        return field;
    }

    public HideField setField(String field) {
        this.field = field;
        return this;
    }
}
