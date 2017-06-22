package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by chambers on 6/19/17.
 */
public class TaxonomyServiceTests extends AbstractTest {

    @Autowired
    TaxonomyService taxonomyService;

    @Autowired
    FolderService folderService;

    @Test
    public void testCreateAndRur() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1.getId()));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1.getId()));

        Source d = new Source();
        d.setId("abc123");
        assetService.index(ImmutableList.of(d));
        refreshIndex();

        folderService.addAssets(folder4, Lists.newArrayList(d.getId()));
        taxonomyService.create(new TaxonomySpec(folder1));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testCreateFailureDuplicate() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1.getId()));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1.getId()));

        taxonomyService.create(new TaxonomySpec(folder1));
        taxonomyService.create(new TaxonomySpec(folder1));
    }


    @Test(expected=ArchivistWriteException.class)
    public void testCreateFailureNested() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1.getId()));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1.getId()));

        taxonomyService.create(new TaxonomySpec(folder1));
        taxonomyService.create(new TaxonomySpec(folder3));
    }

    @Test
    public void testGet() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        Taxonomy tax2 = taxonomyService.get(tax1.getTaxonomyId());
        assertEquals(tax1, tax2);
    }

    @Test
    public void testGetByFolder() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        Taxonomy tax2 = taxonomyService.get(folder1);
        assertEquals(tax1, tax2);
    }

    @Test
    public void testUntagTaxonomy() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        Taxonomy tax2 = taxonomyService.get(folder1);

        String field ="zorroa.taxonomy.tax" + tax2.getTaxonomyId();

        Source d = new Source();
        d.setId("abc123");
        d.setAttr(field,
                ImmutableMap.of("timestamp", 0));
        assetService.index(ImmutableList.of(d));
        refreshIndex();

        Map<String, Long> result = taxonomyService.untagTaxonomy(tax1, System.currentTimeMillis());
        assertEquals(1, result.get("assetCount").longValue());
        assertEquals(0, result.get("errorCount").longValue());
        refreshIndex();

        Asset a = assetService.get(d.getId());
        assertNull(null, a.getAttr(field));
    }

    @Test
    public void testDeleteTaxonomy() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        assertTrue(taxonomyService.delete(tax1));
        assertFalse(taxonomyService.delete(tax1));
    }
}
