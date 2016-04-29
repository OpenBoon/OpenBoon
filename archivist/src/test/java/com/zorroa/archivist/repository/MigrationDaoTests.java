package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Migration;
import com.zorroa.archivist.domain.MigrationType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 2/3/16.
 */
public class MigrationDaoTests extends AbstractTest {

    @Autowired
    MigrationDao migrationDao;

    @Test
    public void testGetAll() {
        int count = jdbc.queryForObject("SELECT COUNT(1) FROM migration", Integer.class);
        assertEquals(count, migrationDao.getAll().size());
    }

    @Test
    public void testGetAllByType() {
        int count = jdbc.queryForObject("SELECT COUNT(1) FROM migration WHERE int_type=?", Integer.class,
                MigrationType.ElasticSearchIndex.ordinal());
        assertEquals(count, migrationDao.getAll(MigrationType.ElasticSearchIndex).size());
    }

    @Test
    public void setVersion() {
        for (Migration m: migrationDao.getAll()) {
            assertTrue(migrationDao.setVersion(m, 1000000));
        }

        for (Migration m: migrationDao.getAll()) {
            assertFalse(migrationDao.setVersion(m, 1000000));
        }
    }
}
