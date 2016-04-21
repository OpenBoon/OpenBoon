package com.zorroa.archivist.repository;

import org.springframework.stereotype.Repository;

import java.io.File;

/**
 * Created by chambers on 4/21/16.
 */
@Repository
public class MaintenanceDaoImpl extends AbstractDao implements MaintenanceDao {

    @Override
    public void backup(File file) {
        jdbc.update("BACKUP TO ?", file.getAbsolutePath());
    }
}
