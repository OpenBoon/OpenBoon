package com.zorroa.common.repository;

import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.common.domain.AnalystState;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 6/16/16.
 */
public interface AnalystDao {
    String register(String id, AnalystSpec builder);

    void setState(String id, AnalystState state);

    Analyst get(String id);

    long count();

    List<Integer> getRunningTaskIds();

    PagedList<Analyst> getAll(Pager paging);

    /**
     * Return a list of analysts that are in the UP state but are not updating
     * their data at regular intervals.
     *
     * @param limit
     * @param duration
     * @return
     */
    List<Analyst> getUnresponsive(int limit, long duration);

    List<Analyst> getActive(Pager paging);

    List<Analyst> getReady(Pager paging);
}
