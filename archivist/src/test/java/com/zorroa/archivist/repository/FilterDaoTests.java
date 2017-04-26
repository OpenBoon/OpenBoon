package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.sdk.search.AssetSearch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 8/10/16.
 */
public class FilterDaoTests extends AbstractTest {

    @Autowired
    FilterDao filterDao;

    Filter filter;
    FilterSpec spec;

    @Before
    public void init() {
        spec = new FilterSpec();
        spec.setDescription("A Filter");
        spec.setEnabled(false);
        spec.setSearch(new AssetSearch());
        spec.setAcl(new Acl());
        filter = filterDao.create(spec);
    }

    @Test
    public void testCreate() {
        assertEquals(spec.getDescription(), filter.getDescription());
        assertEquals(spec.isEnabled(), filter.isEnabled());
    }

    @Test
    public void testGet() {
        Filter f2 = filterDao.get(filter.getId());
        assertEquals(filter, f2);
    }

    @Test
    public void testRefresh() {
        Filter f2 = filterDao.refresh(filter);
        assertEquals(filter, f2);
    }

    @Test
    public void testDelete() {
        long count = filterDao.count();
        filterDao.delete(filter.getId());
        assertEquals(count-1, filterDao.count());
    }

    @Test
    public void testSetEnabled() {
        filterDao.setEnabled(filter.getId(), false);
        assertEquals(0, filterDao.getAll().size());
    }

    @Test
    public void testGetAll() {
        long count = filterDao.count();
        assertEquals(count, filterDao.getAll().size());

        FilterSpec s2= new FilterSpec();
        s2.setDescription("A Filter");
        s2.setEnabled(false);
        spec.setSearch(new AssetSearch());
        spec.setAcl(new Acl());
        filter = filterDao.create(s2);

        assertEquals(count+1, filterDao.getAll().size());
    }
}
