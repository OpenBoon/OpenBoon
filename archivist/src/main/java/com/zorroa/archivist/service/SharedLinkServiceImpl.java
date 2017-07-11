package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.repository.SharedLinkDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by chambers on 7/7/17.
 */
@Service
public class SharedLinkServiceImpl implements SharedLinkService {

    private static final Logger logger = LoggerFactory.getLogger(SharedLinkServiceImpl.class);

    @Autowired
    UserService userService;

    @Autowired
    SharedLinkDao sharedLinkDao;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Override
    public SharedLink create(SharedLinkSpec spec) {
        SharedLink link = sharedLinkDao.create(spec);
        User fromUser = SecurityUtils.getUser();

        if (spec.isSendEmail()) {
            transactionEventManager.afterCommit(() -> {
                for (int userId : spec.getUserIds()) {
                    try {
                        User toUser = userService.get(userId);
                        userService.sendSharedLinkEmail(fromUser, toUser, link);
                    } catch (Exception e) {
                        logger.warn("Failed to send shared link email, id {} ", link.getId(), e);
                    }
                }
            }, true);
        }
        return link;
    }

    @Override
    public SharedLink get(int id) {
        return sharedLinkDao.get(id);
    }
}
