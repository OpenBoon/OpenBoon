package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;
import com.zorroa.archivist.service.FolderService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaxonomyDaoTests extends AbstractTest {

    @Autowired
    TaxonomyDao taxonomyDao;

    @Autowired
    FolderService folderService;

    @Test
    public void testCreate() {
        Folder folder = folderService.create(new FolderSpec("foo"));
        Taxonomy tax = taxonomyDao.create(new TaxonomySpec().setFolderId(folder.getId()));
        assertEquals(tax.getFolderId(), folder.getId());
    }

    @Test
    public void testGet() {
        Folder folder = folderService.create(new FolderSpec("foo"));
        Taxonomy tax1 = taxonomyDao.create(new TaxonomySpec().setFolderId(folder.getId()));
        Taxonomy tax2 = taxonomyDao.get(tax1.getTaxonomyId());
        assertEquals(tax1, tax2);
    }


    @Test
    public void testGetByFolder() {
        Folder folder = folderService.create(new FolderSpec("foo"));
        Taxonomy tax1 = taxonomyDao.create(new TaxonomySpec().setFolderId(folder.getId()));
        Taxonomy tax2 = taxonomyDao.get(folder);
        assertEquals(tax1, tax2);
    }

    @Test
    public void testSetActive() {
        Folder folder = folderService.create(new FolderSpec("foo"));
        Taxonomy tax1 = taxonomyDao.create(new TaxonomySpec().setFolderId(folder.getId()));
        assertTrue(taxonomyDao.setActive(tax1, true));
        assertFalse(taxonomyDao.setActive(tax1, true));
        assertTrue(taxonomyDao.setActive(tax1, false));
        assertFalse(taxonomyDao.setActive(tax1, false));
    }
}
