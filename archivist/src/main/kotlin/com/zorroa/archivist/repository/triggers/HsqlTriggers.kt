package com.zorroa.archivist.repository.triggers

import com.zorroa.archivist.domain.JobState
import org.h2.tools.TriggerAdapter
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class TriggerDecrementFolderChildCount : TriggerAdapter() {

    @Throws(SQLException::class)
    override fun fire(conn: Connection, oldRow: ResultSet?, newRow: ResultSet?) {
        conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count-1 WHERE pk_folder=?"
        ).use { stmt ->
            stmt.setObject(1, oldRow!!.getInt("pk_parent"))
            stmt.executeUpdate()
        }
    }
}

class TriggerIncrementFolderChildCount : TriggerAdapter() {

    @Throws(SQLException::class)
    override fun fire(conn: Connection, oldRow: ResultSet?, newRow: ResultSet?) {
        conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count+1 WHERE pk_folder=?"
        ).use { stmt ->
            stmt.setObject(1, newRow!!.getInt("pk_parent"))
            stmt.executeUpdate()
        }
    }
}

class TriggerUpdateFolderChildCount : TriggerAdapter() {

    @Throws(SQLException::class)
    override fun fire(conn: Connection, oldRow: ResultSet?, newRow: ResultSet?) {

        if (newRow!!.getObject("pk_parent") == null) {
            return
        }

        if (oldRow!!.getObject("pk_parent") == null) {
            return
        }

        val newParent = newRow.getInt("pk_parent")
        val oldParent = oldRow.getInt("pk_parent")

        if (newParent == oldParent) {
            return
        }

        if (newParent > oldParent) {
            updateNewParent(conn, newParent)
            updateOldParent(conn, oldParent)
        } else if (newParent < oldParent) {
            updateOldParent(conn, oldParent)
            updateNewParent(conn, newParent)
        }
    }

    @Throws(SQLException::class)
    private fun updateNewParent(conn: Connection, newParent: Int) {
        conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count+1 WHERE pk_folder=?").use { newStmt ->
            newStmt.setObject(1, newParent)
            newStmt.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun updateOldParent(conn: Connection, oldParent: Int) {
        conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count-1 WHERE pk_folder=?").use { oldStmt ->
            oldStmt.setObject(1, oldParent)
            oldStmt.executeUpdate()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TriggerUpdateFolderChildCount::class.java)
    }

}

class TriggerUpdateJobState : TriggerAdapter() {

    @Throws(SQLException::class)
    override fun fire(conn: Connection, oldRow: ResultSet, newRow: ResultSet) {

        val stateChange = oldRow.getInt("int_task_state_success_count") != newRow.getInt("int_task_state_success_count") ||
                oldRow.getInt("int_task_state_failure_count") != newRow.getInt("int_task_state_failure_count") ||
                oldRow.getInt("int_task_state_skipped_count") != newRow.getInt("int_task_state_skipped_count")

        if (!stateChange) {
            return
        }

        if ((newRow.getInt("int_task_state_success_count")
                + newRow.getInt("int_task_state_failure_count")
                + newRow.getInt("int_task_state_skipped_count")) == newRow.getInt("int_task_total_count")) {

            conn.prepareStatement(
                    "UPDATE job SET int_state=? WHERE pk_job=? AND int_state=?").use { newStmt ->
                newStmt.setInt(1, JobState.Finished.ordinal)
                newStmt.setInt(2, newRow.getInt("pk_job"))
                newStmt.setInt(3, JobState.Active.ordinal)
                newStmt.executeUpdate()
            }

        } else {

            conn.prepareStatement(
                    "UPDATE job SET int_state=? WHERE pk_job=? AND int_state=?").use { newStmt ->
                newStmt.setInt(1, JobState.Active.ordinal)
                newStmt.setInt(2, newRow.getInt("pk_job"))
                newStmt.setInt(3, JobState.Finished.ordinal)
                newStmt.executeUpdate()
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TriggerUpdateJobState::class.java)
    }
}
