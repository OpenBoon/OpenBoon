package com.zorroa.archivist.domain

enum class LogAction {
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

class LogEntry constructor(
        val type:String,
        val action: LogAction,
        val target: Map<String, Any>) {

}
