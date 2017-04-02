package com.zorroa.archivist.repository.triggers;

import org.h2.tools.TriggerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by chambers on 4/2/17.
 */
public class TriggerDecrementFolderChildCount extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE folder SET int_child_count=int_child_count-1 WHERE pk_folder=?"
        )) {
            stmt.setObject(1, oldRow.getInt("pk_parent"));
            stmt.executeUpdate();
        }
    }
}
