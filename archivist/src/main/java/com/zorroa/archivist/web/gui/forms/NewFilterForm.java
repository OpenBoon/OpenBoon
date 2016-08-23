package com.zorroa.archivist.web.gui.forms;

import com.google.common.base.MoreObjects;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by chambers on 8/8/16.
 */
public class NewFilterForm {

    @NotEmpty
    private String description;
    private boolean matchAll = true;

    private List<String> folder;

    private List<String> perm;
    private List<String> read;
    private List<String> write;
    private List<String> export;

    @NotEmpty
    private List<String> attribute;

    @NotEmpty
    private List<String> op;

    private List<String> value;

    public List<String> getPerm() {
        return perm;
    }

    public NewFilterForm setPerm(List<String> perm) {
        this.perm = perm;
        return this;
    }

    public List<String> getRead() {
        return read;
    }

    public NewFilterForm setRead(List<String> read) {
        this.read = read;
        return this;
    }

    public List<String> getWrite() {
        return write;
    }

    public NewFilterForm setWrite(List<String> write) {
        this.write = write;
        return this;
    }

    public List<String> getExport() {
        return export;
    }

    public NewFilterForm setExport(List<String> export) {
        this.export = export;
        return this;
    }

    public List<String> getAttribute() {
        return attribute;
    }

    public NewFilterForm setAttribute(List<String> attribute) {
        this.attribute = attribute;
        return this;
    }

    public List<String> getValue() {
        return value;
    }

    public NewFilterForm setValue(List<String> value) {
        this.value = value;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public NewFilterForm setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<String> getFolder() {
        return folder;
    }

    public NewFilterForm setFolder(List<String> folder) {
        this.folder = folder;
        return this;
    }

    public List<String> getOp() {
        return op;
    }

    public NewFilterForm setOp(List<String> op) {
        this.op = op;
        return this;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public NewFilterForm setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("perm", perm)
                .add("read", read)
                .add("write", write)
                .add("export", export)
                .add("attribute", attribute)
                .add("value", value)
                .toString();
    }
}
