package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Migration;
import com.zorroa.archivist.domain.MigrationType;

import java.util.List;

/**
 * Created by chambers on 2/2/16.
 */
public interface MigrationService {

    void processAll();

    List<Migration> getAll();
    List<Migration> getAll(MigrationType type);

    void processMigrations(List<Migration> migrations, boolean force);

    void processElasticMigration(Migration m, boolean force);
}
