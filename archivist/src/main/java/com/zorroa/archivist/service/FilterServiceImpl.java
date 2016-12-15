package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.repository.FilterDao;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by chambers on 8/9/16.
 */
@Service
@Transactional
public class FilterServiceImpl implements FilterService {

    private static final Logger logger = LoggerFactory.getLogger(FilterServiceImpl.class);

    @Autowired
    FilterDao filterDao;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    LogService logService;

    @Override
    public Filter create(FilterSpec spec) {
        Filter filter = filterDao.create(spec);
        transactionEventManager.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build(LogAction.Create,
                    "filter", filter.getId()));
        });

        return filter;
    }

    @Override
    public List<Filter> getAll() {
        return filterDao.getAll();
    }

    @Override
    public PagedList<Filter> getPaged(Pager page) {
        return filterDao.getAll(page);
    }
}
