package com.zorroa.common.repository;

import com.zorroa.common.domain.*;

/**
 * Created by chambers on 12/28/15.
 */
public interface EventLogDao {

    PagedList<Event> getAll(EventSearch search);

    PagedList<Event> getAll(Paging paging);

    PagedList<Event> getAll(EventSearch search, Paging paging);

    /**
     * Execute a count query in the event log.
     *
     * @param search
     * @return
     */
    long count(EventSearch search);

    void setSynchronous(boolean synchronous);

    void info(EventSpec msg);

    void warn(EventSpec msg);

    void error(EventSpec msg);
}
