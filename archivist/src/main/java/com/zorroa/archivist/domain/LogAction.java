package com.zorroa.archivist.domain;

/**
 * These all get lowercased once they go into elastic.
 */
public enum LogAction {
    Search,
    Create,
    Update,
    Delete,
    Get,
    Export,
    Import,
    Login,
    LoginFailure,
    BulkUpdate,
    View
}
