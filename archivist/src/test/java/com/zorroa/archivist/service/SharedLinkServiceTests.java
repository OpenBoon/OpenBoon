package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.zorroa.archivist.security.UtilsKt.getUserId;

/**
 * Created by chambers on 7/11/17.
 */
public class SharedLinkServiceTests extends AbstractTest {

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    SharedLinkService sharedLinkService;

    @Test
    public void testSendEmail() throws InterruptedException {
        transactionEventManager.setImmediateMode(true);

        SharedLinkSpec spec = new SharedLinkSpec();
        spec.setSendEmail(true);
        spec.setState(ImmutableMap.of("foo", "bar"));
        spec.setUserIds(ImmutableSet.of(getUserId()));
        spec.setExpireTimeMs(1L);
        SharedLink link = sharedLinkService.create(spec);

        Thread.sleep(2000);
    }
}
