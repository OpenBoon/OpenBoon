package com.zorroa.archivist.sdk.domain;

public enum MessageType {

    /*
     * TODO: comment the purpose of each type.
     */

    SESSION,
    ASSET_GET,
    ASSET_UPDATE,
    INGEST_CREATE,
    INGEST_START,
    INGEST_UPDATE_COUNTERS,
    INGEST_UPDATE,
    INGEST_STOP,
    INGEST_DELETE,
    INGEST_EXCEPTION,
    FOLDER_CREATE,
    FOLDER_UPDATE,
    FOLDER_DELETE,
    FOLDER_ADD_ASSETS,
    FOLDER_REMOVE_ASSETS,
    EXPORT_START,
    EXPORT_STOP,
    EXPORT_ASSET,
    EXPORT_OUTPUT_STOP,
    EXPORT_EXCEPTION,

    /**
     * Emitted when the room selection has changed.
     */
    ROOM_SELECTION_UPDATE,

    /**
     * Emitted when the room search has changed.
     */
    ROOM_SEARCH_UPDATE,

    /**
     * Emitted to the room when a user joins.
     */
    ROOM_USER_JOINED,

    /**
     * Emitted to room when user leaves.
     */
    ROOM_USER_LEFT,

    /**
     * A room was updated.  This means some other property of the room changed
     * besides the selection or update.
     */
    ROOM_UPDATED
}
