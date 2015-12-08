package com.zorroa.archivist.sdk.domain;

public enum MessageType {

    /*
     * TODO: comment the purpose of each type.
     */

    SESSION,
    @Deprecated
    ASSET_SEARCH,
    ASSET_GET,
    ASSET_UPDATE,
    ASSET_UPDATE_FOLDERS,
    @Deprecated
    ASSET_SELECT,
    @Deprecated
    ASSET_DESELECT,
    INGEST_CREATE,
    INGEST_START,
    INGEST_UPDATE_COUNTERS,
    INGEST_UPDATE,
    INGEST_STOP,
    INGEST_DELETE,
    FOLDER_CREATE,
    FOLDER_UPDATE,
    FOLDER_DELETE,
    EXPORT_START,
    EXPORT_STOP,
    EXPORT_ASSET,
    EXPORT_OUTPUT_STOP,

    /**
     * Emitted when the room selection has changed.
     */
    ROOM_SELECTION_UPDATE,

    /**
     * Emitted when the room search has changed.
     */
    ROOM_SEARCH_UPDATE
}
