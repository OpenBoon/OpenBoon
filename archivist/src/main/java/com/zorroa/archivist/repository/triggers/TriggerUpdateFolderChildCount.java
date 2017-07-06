package com.zorroa.archivist.repository.triggers;

import org.h2.tools.TriggerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Created by chambers on 4/2/17.
 */
public class TriggerUpdateFolderChildCount extends TriggerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TriggerUpdateFolderChildCount.class);

    @Override
    public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {

        if(newRow.getObject("pk_parent") == null) {
            return;
        }

        if(oldRow.getObject("pk_parent") == null) {
            return;
        }

        int newParent = newRow.getInt("pk_parent");
        int oldParent = oldRow.getInt("pk_parent");

        if (newParent == oldParent) {
            return;
        }

        if (newParent > oldParent) {
            updateNewParent(conn, newParent);
            updateOldParent(conn, oldParent);
        }
        else if (newParent < oldParent) {
            updateOldParent(conn, oldParent);
            updateNewParent(conn, newParent);
        }
    }

    private void updateNewParent(Connection conn, int newParent) throws SQLException {
        try (PreparedStatement newStmt = conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count+1 WHERE pk_folder=?")) {
            newStmt.setObject(1, newParent);
            newStmt.executeUpdate();
        }
    }

    private void updateOldParent(Connection conn, int oldParent) throws SQLException  {
        try (PreparedStatement oldStmt = conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count-1 WHERE pk_folder=?");
        ) {
            oldStmt.setObject(1, oldParent);
            oldStmt.executeUpdate();
        }
    }

}
