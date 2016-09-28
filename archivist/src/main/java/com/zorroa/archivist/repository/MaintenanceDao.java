package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.ExpiredJob;

import java.io.File;
import java.util.List;

/**
 * Created by chambers on 4/21/16.
 */
public interface MaintenanceDao {

    void backup(File file);

    List<ExpiredJob> getExpiredJobs(long expireTime);

    void removeTaskData(ExpiredJob job);
}
