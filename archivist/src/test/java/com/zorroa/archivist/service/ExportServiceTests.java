package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.ExportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 8/16/16.
 */
public class ExportServiceTests extends AbstractTest {

    @Autowired
    ExportService exportService;

    @Autowired
    PluginService pluginService;

    @Autowired
    AssetDao assetDao;

    Job job;
    ExportSpec spec;
    Asset asset;

    @Before
    public void init() {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", "cat");
        asset = assetDao.index(source);
        refreshIndex();

        spec = new ExportSpec();
        spec.setName("test");
        spec.setSearch(new AssetSearch().setQuery("cats"));
        job = exportService.create(spec);
    }

    @Test
    public void testCreate() {
        /**
         * Export jobs are inline
         */
        int count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(1, count);



    }


}
