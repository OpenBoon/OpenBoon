package com.zorroa.archivist.domain;

public class SnapshotRestoreBuilder {

    private String indices;
    private String renamePattern;
    private String renameReplacement;

    public String getIndices() {
        return indices;
    }

    public void setIndices(String indices) {
        this.indices = indices;
    }

    public String getRenamePattern() {
        return renamePattern;
    }

    public void setRenamePattern(String renamePattern) {
        this.renamePattern = renamePattern;
    }

    public String getRenameReplacement() {
        return renameReplacement;
    }

    public void setRenameReplacement(String renameReplacement) {
        this.renameReplacement = renameReplacement;
    }

}
