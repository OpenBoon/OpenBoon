package com.zorroa.cloudproxy.service;

import java.util.Date;

/**
 * Created by chambers on 3/24/17.
 */
public interface SchedulerService {

    Date getNextRunTime();

    void reloadAndRestart(boolean allowStartNow);
}
