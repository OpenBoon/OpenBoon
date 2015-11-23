package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.SecurityUtils;
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

        AssetSearchBuilder search = new AssetSearchBuilder();
        search.setQuery("foo");

        ExportBuilder builder = new ExportBuilder();
        builder.setOptions(options);
        builder.setSearch(search);

        export = exportDao.create(builder);
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
}
