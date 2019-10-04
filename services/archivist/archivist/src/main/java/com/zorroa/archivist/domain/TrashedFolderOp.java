package com.zorroa.archivist.domain;

import java.util.UUID;

/**
 * Created by chambers on 12/5/16.
 */
public class TrashedFolderOp {

    UUID trashFolderId;
    String opId;
    int count;

    public TrashedFolderOp() { }

    public TrashedFolderOp(UUID trashFolderId, String opId, int count) {
        this.trashFolderId = trashFolderId;
        this.opId = opId;
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public TrashedFolderOp setCount(int count) {
        this.count = count;
        return this;
    }

    public UUID getTrashFolderId() {
        return trashFolderId;
    }

    public TrashedFolderOp setTrashFolderId(UUID trashFolderId) {
        this.trashFolderId = trashFolderId;
        return this;
    }

    public String getOpId() {
        return opId;
    }

    public TrashedFolderOp setOpId(String opId) {
        this.opId = opId;
        return this;
    }
}
