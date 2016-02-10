package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import com.zorroa.archivist.sdk.domain.AnalystState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by chambers on 2/9/16.
 */
@Service
@Transactional
public class AnalystServiceImpl implements AnalystService {

    @Autowired
    AnalystDao analystDao;

    @Override
    public void register(AnalystPing ping) {
        if (!analystDao.update(ping)) {
            analystDao.create(ping);
        }
        else {
            /*
             * Reset the host state to up.  Eventually, we'll only want to move the state to up
             * if the node is in a certain state, which is why this isn't part of the
             * standard update.
             */
            analystDao.setState(ping.getHost(), AnalystState.UP);
        }
    }

    @Override
    public void shutdown(AnalystPing ping) {
        /*
         * Do a final update then then set the state to shutdown.
         */
        if (analystDao.update(ping)) {
            analystDao.setState(ping.getHost(), AnalystState.SHUTDOWN, AnalystState.UP);
        }
    }
}
