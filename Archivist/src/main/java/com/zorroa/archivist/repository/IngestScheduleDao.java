package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;

import java.util.List;

/**
 * Created by chambers on 9/5/15.
 */
public interface IngestScheduleDao {

    IngestSchedule create(IngestScheduleBuilder builder);

    IngestSchedule get(int id);

    List<IngestSchedule> getAll();

    List<IngestSchedule> getAllReady();

    void started(IngestSchedule schedule);

    void mapScheduleToIngests(IngestSchedule schedule, List<Long> ingests);

    boolean update(IngestSchedule schedule);
}
