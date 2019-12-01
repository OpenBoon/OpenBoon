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
    EXPAND,
    SCAN
}

/**
 * All the different classes we're creating event logs for.
 */
enum class LogObject {
    ASSET,
    INDEX_ROUTE,
    INDEX_CLUSTER,
    JOB,
    PIPELINE,
    STORAGE,
    TASK,
    ANALYST,
    TASK_ERROR,
    PROJECT,
    DATASOURCE
}
