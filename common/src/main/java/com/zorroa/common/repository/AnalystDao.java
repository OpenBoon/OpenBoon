package com.zorroa.common.repository;

import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystBuilder;
import com.zorroa.common.domain.AnalystUpdateBuilder;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 6/16/16.
 */
public interface AnalystDao {
    String register(AnalystBuilder builder);

    void update(String id, AnalystUpdateBuilder builder);

    Analyst get(String id);

    long count();

    PagedList<Analyst> getAll(Pager paging);

    List<Analyst> getActive(Pager paging);

    List<Analyst> getActive(Pager paging, int maxQueueSize);
}
