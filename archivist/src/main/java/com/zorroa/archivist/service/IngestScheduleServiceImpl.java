package com.zorroa.archivist.service;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;
import com.zorroa.archivist.repository.IngestDao;
import com.zorroa.archivist.repository.IngestScheduleDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 9/17/15.
 */
@Transactional
@Service
public class IngestScheduleServiceImpl extends AbstractScheduledService implements IngestScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(IngestScheduleServiceImpl.class);

    @Autowired
    IngestScheduleDao ingestScheduleDao;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    IngestDao ingestDao;

    @Override
    public IngestSchedule create(IngestScheduleBuilder builder) {
        return ingestScheduleDao.create(builder);
    }

    @Override
    public IngestSchedule get(int id) {
        return ingestScheduleDao.get(id);
    }

    @Override
    public boolean update(IngestSchedule schedule) {
        return ingestScheduleDao.update(schedule);
    }

    @Override
    public List<IngestSchedule> getAll() {
        return ingestScheduleDao.getAll();
    }

    @Override
    public List<IngestSchedule> getAllReady() {
        return ingestScheduleDao.getAllReady();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public int executeReady() {
        int result = 0;
        for (IngestSchedule schedule: getAllReady()) {
            ingestScheduleDao.started(schedule);
            ingestDao.getAll(schedule).forEach(i -> ingestExecutorService.start(i));
            result += schedule.getIngestIds().size();
        }
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    protected void runOneIteration() throws Exception {

        if (ArchivistConfiguration.unittest) {
            return;
        }

        try {
            executeReady();
        } catch (Exception e) {
            logger.warn("Failed to run scheduled ingests: ", e);
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(10, 10, TimeUnit.SECONDS);
    }
}
