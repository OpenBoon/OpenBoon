package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
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

    JobSpec spec;
    Job job;
    @Before
    public void init() {

        spec = new JobSpec();
        spec.setName("job");
        spec.setType(PipelineType.Import);
        spec.setLogPath(properties.getString("archivist.path.logs"));
        job = jobDao.create(spec);
    }

    @Test
    public void testCreate() {
        assertNotNull(job.getJobId());
    }

    @Test
    public void testGet() {
        Job j = jobDao.get(job.getJobId());
        validate(j);
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
        assertTrue(jobDao.setState(job, JobState.Finished, JobState.Active));
        assertFalse(jobDao.setState(job, JobState.Finished, JobState.Active));
    }

    @Test
    public void testIncrementStats() {
        jobDao.incrementStats(job.getJobId(), 1, 2, 3);
        Job j = jobDao.get(job.getJobId());
        assertEquals(1, j.getStats().getFrameSuccessCount());
        assertEquals(2, j.getStats().getFrameErrorCount());
        assertEquals(3, j.getStats().getFrameWarningCount());
    }

    public void validate(Job job) {
        assertEquals(job.getName(), job.getName());
    }
}
