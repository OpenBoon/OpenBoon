package com.zorroa.archivist.domain

/**
 * All the different actions we can take against any data within the Archivist.
 */
enum class LogAction {
    CREATE,
    REPLACE,
    UPDATE,
    DELETE,
    WRITE,
    GET,
    INDEX,
    EXECUTE,
    AUTHENTICATE,
    AUTHORIZE,
    SECURE,
    BATCH_CREATE,
    BATCH_UPDATE,
    BATCH_DELETE,
    BATCH_INDEX,
    ERROR,
    WARN,
    STATE_CHANGE,
    UPLOAD,
    KILL,
    STREAM,
    LOCK,
    UNLOCK,
    APIKEY,
    EXPAND
}


/**
 * All the different classes we're creating event logs for.
 */
enum class LogObject {
    ASSET,
    BLOB,
    CLUSTER_LOCK,
    DYHI,
    EXPORT_FILE,
    FILEQ,
    FOLDER,
    JOB,
    ORGANIZATION,
    PERMISSION,
    PIPELINE,
    PROCESSOR,
    STORAGE,
    TASK,
    TAXONOMY,
    TRASH_FOLDER,
    USER,
    ANALYST
}

