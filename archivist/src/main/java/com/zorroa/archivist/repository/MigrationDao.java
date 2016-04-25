package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Migration;
import com.zorroa.archivist.domain.MigrationType;

import java.util.List;

/**
 * Created by chambers on 2/2/16.
 */
public interface MigrationDao {

    List<Migration> getAll();

    List<Migration> getAll(MigrationType type);

    /**
     * Get the version of the migration to the given version.  If the version
     * changes, return true otherwise return false.
     *
     * @param m
     * @param version
     * @return
     */
    boolean setVersion(Migration m, int version);
}
