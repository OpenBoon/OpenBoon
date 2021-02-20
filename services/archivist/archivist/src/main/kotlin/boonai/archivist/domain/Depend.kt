package boonai.archivist.domain

import java.util.UUID

enum class DependState {
    Inactive,
    Active
}

enum class DependType {
    JobOnJob,
    TaskOnTask
}

class Depend(
    val id: UUID,
    val state: DependState,
    val type: DependType,
    val dependErJobId: UUID,
    val dependOnJobId: UUID,
    val dependErTaskId: UUID?,
    val dependOnTaskId: UUID?
)

class DependSpec(
    val type: DependType,
    val dependErJobId: UUID,
    val dependOnJobId: UUID,
    val dependErTaskId: UUID? = null,
    val dependOnTaskId: UUID? = null
)
