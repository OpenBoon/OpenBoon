package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobFilter;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/12/16.
 */
public class JobDaoTests extends AbstractTest {

    @Autowired
    JobDao jobDao;

    ZpsScript script;

    @Before
    public void init() {
        script = new ZpsScript();
        script.setName("foo-bar");
        jobDao.create(script, PipelineType.Import);
    }

    @Test
    public void testCreate() {
        assertNotNull(script.getJobId());
    }

    @Test
    public void testGet() {
        Job job = jobDao.get(script);
        validate(job);
    }

    @Test
    public void testCount() {
        assertEquals(1, jobDao.count());
        assertEquals(1, jobDao.count(new JobFilter()));
        assertEquals(1, jobDao.count(new JobFilter().setState(JobState.Active)));
        assertEquals(0, jobDao.count(new JobFilter().setState(JobState.Finished)));
        assertEquals(1, jobDao.count(new JobFilter().setType(PipelineType.Import)));
        assertEquals(0, jobDao.count(new JobFilter().setType(PipelineType.Export)));
    }

    @Test
    public void testGetAll() {
        PagedList<Job> jobs = jobDao.getAll(Paging.first(), new JobFilter());
        assertEquals(1, jobs.getList().size());

        jobs = jobDao.getAll(Paging.first(), new JobFilter().setState(JobState.Finished));
        assertEquals(0, jobs.getList().size());

        jobs = jobDao.getAll(Paging.first(), new JobFilter().setState(JobState.Active));
        assertEquals(1, jobs.getList().size());
    }

    @Test
    public void testSetState() {
        assertTrue(jobDao.setState(script, JobState.Finished, JobState.Active));
        assertFalse(jobDao.setState(script, JobState.Finished, JobState.Active));
    }

    @Test
    public void testIncrementStats() {
        jobDao.incrementStats(script.getJobId(), 1, 2, 3, 4);
        Job job = jobDao.get(script);
        assertEquals(6, job.getStats().getAssetTotal());
        assertEquals(1, job.getStats().getAssetCreated());
        assertEquals(2, job.getStats().getAssetUpdated());
        assertEquals(3, job.getStats().getAssetErrored());
        assertEquals(4, job.getStats().getAssetWarning());
    }

    public void validate(Job job) {
        assertEquals(script.getName(), job.getName());
    }
}
