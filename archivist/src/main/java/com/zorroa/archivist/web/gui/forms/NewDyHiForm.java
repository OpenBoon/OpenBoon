package com.zorroa.archivist.web.gui.forms;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by chambers on 8/10/16.
 */
public class NewDyHiForm {

    @NotNull
    private Integer folderId;

    private List<String> type;

    private List<String> field;

    public Integer getFolderId() {
        return folderId;
    }

    public NewDyHiForm setFolderId(Integer folderId) {
        this.folderId = folderId;
        return this;
    }

    public List<String> getType() {
        return type;
    }

    public NewDyHiForm setType(List<String> type) {
        this.type = type;
        return this;
    }

    public List<String> getField() {
        return field;
    }

    public NewDyHiForm setField(List<String> field) {
        this.field = field;
        return this;
    }
}
