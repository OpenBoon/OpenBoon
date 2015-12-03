package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.sdk.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 11/12/15.
 */
public class ExportDaoTests extends ArchivistApplicationTests {

    @Autowired
    ExportDao exportDao;

    Export export;

    @Before
    public void init() {

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);

        AssetSearch search = new AssetSearch();
        search.setQuery("foo");

        ExportBuilder builder = new ExportBuilder();
        builder.setOptions(options);
        builder.setSearch(search);

        export = exportDao.create(builder, 0, 0);
    }

    @Test
    public void testGet() {
        Export export2 = exportDao.get(export.getId());
        assertEquals(export, export2);
    }

    @Test
    public void testGetAll() {
        List<Export> exports = exportDao.getAll(ExportState.Queued, 10);
        assertEquals(export, exports.get(0));
    }

    @Test
    public void testGetAllByFilter_Empty() {
        ExportFilter filter = new ExportFilter();
        List<Export> exports = exportDao.getAll(filter);
        assertEquals(export, exports.get(0));
    }

    @Test
    public void testGetAllByFilter_User() {
        ExportFilter filter = new ExportFilter();
        filter.setUsers(Lists.newArrayList(123));
        List<Export> exports = exportDao.getAll(filter);
        assertEquals(0, exports.size());

        filter.setUsers(Lists.newArrayList(SecurityUtils.getUser().getId(), 999, 123));
        exports = exportDao.getAll(filter);
        assertEquals(1, exports.size());
    }

    @Test
    public void testGetAllByFilter_State() {
        ExportFilter filter = new ExportFilter();
        filter.setStates(Lists.newArrayList(ExportState.Running));
        List<Export> exports = exportDao.getAll(filter);
        assertEquals(0, exports.size());

        filter.setStates(Lists.newArrayList(ExportState.Queued, ExportState.Running));
        exports = exportDao.getAll(filter);
        assertEquals(1, exports.size());
    }

    @Test
    public void testGetAllByFilter_AfterTime() {
        ExportFilter filter = new ExportFilter();
        filter.setAfterTime(System.currentTimeMillis() + 100000);
        List<Export> exports = exportDao.getAll(filter);
        assertEquals(0, exports.size());

        filter.setAfterTime(System.currentTimeMillis() - 100000);
        exports = exportDao.getAll(filter);
        assertEquals(1, exports.size());
    }

    @Test
    public void testGetAllByFilter_BeforeTime() {
        ExportFilter filter = new ExportFilter();
        filter.setBeforeTime(System.currentTimeMillis() - 100000);
        List<Export> exports = exportDao.getAll(filter);
        assertEquals(0, exports.size());

        filter.setBeforeTime(System.currentTimeMillis() + 100000);
        exports = exportDao.getAll(filter);
        assertEquals(1, exports.size());
    }

    @Test
    public void testSetState() {
        assertTrue(exportDao.setState(export, ExportState.Running, ExportState.Queued));
        assertFalse(exportDao.setState(export, ExportState.Running, ExportState.Queued));
    }

    @Test
    public void testSetStarted() {
        int executeCount = jdbc.queryForObject("SELECT int_execute_count FROM export WHERE pk_export=?",
                Integer.class, export.getId());
        assertEquals(0, executeCount);

        assertTrue(exportDao.setRunning(export));
        executeCount = jdbc.queryForObject("SELECT int_execute_count FROM export WHERE pk_export=?",
                Integer.class, export.getId());
        assertEquals(1, executeCount);

        assertFalse(exportDao.setRunning(export));
        executeCount = jdbc.queryForObject("SELECT int_execute_count FROM export WHERE pk_export=?",
                Integer.class, export.getId());
        assertEquals(1, executeCount);
    }

    @Test
    public void testSetStopped() {
        long timeStopped = jdbc.queryForObject("SELECT time_stopped FROM export WHERE pk_export=?",
                Long.class, export.getId());
        assertEquals(-1, timeStopped);
        assertFalse(exportDao.setFinished(export));

        assertTrue(exportDao.setRunning(export));
        timeStopped = jdbc.queryForObject("SELECT time_stopped FROM export WHERE pk_export=?",
                Long.class, export.getId());
        assertEquals(-1, timeStopped);

        assertTrue(exportDao.setFinished(export));
        timeStopped = jdbc.queryForObject("SELECT time_stopped FROM export WHERE pk_export=?",
                Long.class, export.getId());
        assertTrue(timeStopped > -1);
    }


    @Test
    public void testSetQueued() {
        long timeStopped = jdbc.queryForObject("SELECT time_stopped FROM export WHERE pk_export=?",
                Long.class, export.getId());
        assertEquals(-1, timeStopped);
        assertFalse(exportDao.setQueued(export));

        assertTrue(exportDao.setRunning(export));
        timeStopped = jdbc.queryForObject("SELECT time_stopped FROM export WHERE pk_export=?",
                Long.class, export.getId());
        assertEquals(-1, timeStopped);

        assertTrue(exportDao.setFinished(export));
        timeStopped = jdbc.queryForObject("SELECT time_stopped FROM export WHERE pk_export=?",
                Long.class, export.getId());
        assertTrue(timeStopped > -1);

        assertTrue(exportDao.setQueued(export));
        timeStopped = jdbc.queryForObject("SELECT time_stopped FROM export WHERE pk_export=?",
                Long.class, export.getId());
        assertEquals(-1, timeStopped);
    }


    @Test
    public void testSetSearch() {
        AssetSearch newSearch = new AssetSearch("bar");
        assertTrue(exportDao.setSearch(export, newSearch));

        Export export2 = exportDao.get(export.getId());
        assertEquals("bar", export2.getSearch().getQuery());
    }
}
