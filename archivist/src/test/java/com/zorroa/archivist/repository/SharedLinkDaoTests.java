package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;
import com.zorroa.archivist.service.TransactionEventManager;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 7/11/17.
 */
public class SharedLinkDaoTests extends AbstractTest {

    @Autowired
    SharedLinkDao sharedLinkDao;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Test
    public void testCreate() {
        SharedLinkSpec spec = new SharedLinkSpec();
        spec.setSendEmail(true);
        spec.setState(ImmutableMap.of("foo", "bar"));
        spec.setUserIds(ImmutableSet.of(1));
        spec.setExpireTimeMs(1L);
        SharedLink link = sharedLinkDao.create(spec);
        assertEquals(spec.getState(), link.getState());
        assertTrue(link.getExpireTime() > 0);
    }

    @Test
    public void testGet() {
        SharedLinkSpec spec = new SharedLinkSpec();
        spec.setSendEmail(true);
        spec.setState(ImmutableMap.of("foo", "bar"));
        spec.setUserIds(ImmutableSet.of(1));
        spec.setExpireTimeMs(1L);
        SharedLink link1 = sharedLinkDao.create(spec);
        SharedLink link2 = sharedLinkDao.get(link1.getId());
        assertEquals(link1, link2);
    }

    @Test
    public void testDeleteExpiredMiss() {
        SharedLinkSpec spec = new SharedLinkSpec();
        spec.setSendEmail(true);
        spec.setState(ImmutableMap.of("foo", "bar"));
        spec.setUserIds(ImmutableSet.of(1));
        spec.setExpireTimeMs(86400 * 1000L);
        SharedLink link = sharedLinkDao.create(spec);

        assertEquals(0, sharedLinkDao.deleteExpired(System.currentTimeMillis()));
        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(1) FROM shared_link", Integer.class));
    }

    @Test
    public void testDeleteExpiredHit() {
        SharedLinkSpec spec = new SharedLinkSpec();
        spec.setSendEmail(true);
        spec.setState(ImmutableMap.of("foo", "bar"));
        spec.setUserIds(ImmutableSet.of(1));
        spec.setExpireTimeMs(1L);
        SharedLink link = sharedLinkDao.create(spec);

        assertEquals(1, sharedLinkDao.deleteExpired(System.currentTimeMillis()+10));
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(1) FROM shared_link", Integer.class));
    }
}
