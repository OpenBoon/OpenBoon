package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.ExportFileSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobSpec;
import com.zorroa.archivist.domain.PipelineType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ExportDaoTests extends AbstractTest {

    @Autowired
    JobDao jobDao;

    @Autowired
    ExportDao exportDao;


    JobSpec spec;
    Job job;
    @Before
    public void init() {
        spec = new JobSpec();
        spec.setName("job");
        spec.setType(PipelineType.Import);
        spec.setRootPath("/tmp/archivist");
        job = jobDao.create(spec);
    }

    @Test
    public void testCreateExportFile() {
        ExportFileSpec spec = new ExportFileSpec();
        spec.setMimeType("application/octet-stream");
        spec.setName("bob.zip");
        spec.setSize(1022123);

        exportDao.createExportFile(job, spec);
    }
}
