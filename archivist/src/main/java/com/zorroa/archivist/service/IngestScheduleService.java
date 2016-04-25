package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;

import java.util.List;

/**
 * Created by chambers on 9/17/15.
 */

public interface IngestScheduleService {

    IngestSchedule create(IngestScheduleBuilder builder);

    IngestSchedule get(int id);

    boolean update(IngestSchedule schedule);

    List<IngestSchedule> getAll();

    List<IngestSchedule> getAllReady();

    int executeReady();
}
