package com.zorroa.archivist.repository.triggers;

import org.h2.tools.TriggerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by chambers on 4/2/17.
 */
public class TriggerUpdateFolderChildCount extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
        Integer newParent = newRow.getInt("pk_parent");
        Integer oldParent = oldRow.getInt("pk_parent");

        if (newParent == oldParent) {
            return;
        }

        try (PreparedStatement oldStmt = conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count-1 WHERE pk_folder=?");
             PreparedStatement newStmt = conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count+1 WHERE pk_folder=?")

        ) {
            if (oldParent != null) {
                oldStmt.setObject(1, oldParent);
                oldStmt.executeUpdate();
            }

            if (newParent != null) {
                newStmt.setObject(1, newParent);
                newStmt.executeUpdate();
            }
        }
    }
}
