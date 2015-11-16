package com.zorroa.archivist.repository;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void testSetState() {
        assertTrue(exportDao.setState(export, ExportState.Running, ExportState.Queued));
        assertFalse(exportDao.setState(export, ExportState.Running, ExportState.Queued));
    }
}
