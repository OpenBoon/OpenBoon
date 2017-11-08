package com.zorroa.archivist.repository.triggers;

import com.zorroa.archivist.domain.JobState;
import org.h2.tools.TriggerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by chambers on 4/2/17.
 */
public class TriggerUpdateJobState extends TriggerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TriggerUpdateJobState.class);

    @Override
    public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {

        boolean stateChange =
                oldRow.getInt("int_task_state_success_count") !=
                        newRow.getInt("int_task_state_success_count") ||
                oldRow.getInt("int_task_state_failure_count") !=
                        newRow.getInt("int_task_state_failure_count") ||
                oldRow.getInt("int_task_state_skipped_count") !=
                        newRow.getInt("int_task_state_skipped_count");

        if (!stateChange) {
            return;
        }

        if ((newRow.getInt("int_task_state_success_count")
                + newRow.getInt("int_task_state_failure_count")
                + newRow.getInt("int_task_state_skipped_count")) ==
                newRow.getInt("int_task_total_count")) {

            try (PreparedStatement newStmt = conn.prepareStatement(
                    "UPDATE job SET int_state=? WHERE pk_job=? AND int_state=?")) {
                newStmt.setInt(1, JobState.Finished.ordinal());
                newStmt.setInt(2, newRow.getInt("pk_job"));
                newStmt.setInt(3, JobState.Active.ordinal());
                newStmt.executeUpdate();
            }

        } else {

            try (PreparedStatement newStmt = conn.prepareStatement(
                    "UPDATE job SET int_state=? WHERE pk_job=? AND int_state=?")) {
                newStmt.setInt(1, JobState.Active.ordinal());
                newStmt.setInt(2, newRow.getInt("pk_job"));
                newStmt.setInt(3, JobState.Finished.ordinal());
                newStmt.executeUpdate();
            }
        }
    }
}
