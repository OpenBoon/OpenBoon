package com.zorroa.common.repository;

import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystBuilder;
import com.zorroa.sdk.domain.AnalystUpdateBuilder;

import java.util.List;

/**
 * Created by chambers on 6/16/16.
 */
public interface AnalystDao {
    String register(AnalystBuilder builder);

    void update(String id, AnalystUpdateBuilder builder);

    Analyst get(String id);

    long count();

    List<Analyst> getAll(Paging paging);

    List<Analyst> getActive(Paging paging);
}
