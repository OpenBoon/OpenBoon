package boonai.archivist.repository

import boonai.archivist.domain.IndexTask
import boonai.archivist.domain.IndexTaskState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface IndexTaskDao : JpaRepository<IndexTask, UUID> {

    fun getAllByStateOrderByTimeCreatedDesc(state: IndexTaskState): List<IndexTask>
}

interface IndexTaskJdbcDao {
    /**
     * Update the state of an [IndexTask]
     */
    fun updateState(task: IndexTask, state: IndexTaskState): Boolean

    /**
     * Delete any expired index tasks.  Expired tasks have a time older than the given time.
     */
    fun deleteExpiredTasks(time: Long): Int
}

@Repository
class IndexTaskJdbcDaoImpl : AbstractDao(), IndexTaskJdbcDao {

    override fun updateState(task: IndexTask, state: IndexTaskState): Boolean {
        return jdbc.update(UPDATE_STATE, state.ordinal, System.currentTimeMillis(), task.id, state.ordinal) == 1
    }

    override fun deleteExpiredTasks(time: Long): Int {
        return jdbc.update(DELETE_EXP, time)
    }

    companion object {
        private const val UPDATE_STATE =
            "UPDATE index_task SET int_state=?, time_modified=? WHERE pk_index_task=? AND int_state != ?"

        private const val DELETE_EXP = "DELETE FROM index_task WHERE time_created  < ?"
    }
}
