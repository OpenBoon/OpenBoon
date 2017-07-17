package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
@Service
public class AnalystServiceImpl implements AnalystService {

    private static final Logger logger = LoggerFactory.getLogger(AnalystServiceImpl.class);

    @Autowired
    AnalystDao analystDao;

    @Autowired
    TaskDao taskDao;

    @Override
    public void register(AnalystSpec spec) {
        analystDao.register(spec);
        if (spec.getTaskIds() != null) {
            taskDao.updatePingTime(spec.getTaskIds());
            if (logger.isDebugEnabled()) {
                logger.debug("updated {} task Ids for {}", spec.getTaskIds(), spec.getUrl());
            }
        }
    }

    @Override
    public Analyst get(String url) {
        return analystDao.get(url);
    }

    @Override
    public int getCount() {
        return Math.toIntExact(analystDao.count());
    }

    @Override
    public List<Analyst> getActive() {
        return analystDao.getActive(new Pager(1, 100));
    }

    @Override
    public PagedList<Analyst> getAll(Pager paging) {
        return analystDao.getAll(paging);
    }


}
