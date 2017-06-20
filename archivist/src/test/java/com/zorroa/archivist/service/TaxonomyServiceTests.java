package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.Taxonomy;
import com.zorroa.archivist.domain.TaxonomySpec;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.processor.Source;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 6/19/17.
 */
public class TaxonomyServiceTests extends AbstractTest {

    @Autowired
    TaxonomyService taxonomyService;

    @Test
    public void testCreate() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1.getId()));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1.getId()));

        Source d = new Source();
        d.setId("abc123");
        assetService.index(ImmutableList.of(d));
        refreshIndex();

        folderService.addAssets(folder4, Lists.newArrayList(d.getId()));
        taxonomyService.createTaxonomy(new TaxonomySpec(folder1));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testCreateFailureDuplicate() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1.getId()));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1.getId()));

        taxonomyService.createTaxonomy(new TaxonomySpec(folder1));
        taxonomyService.createTaxonomy(new TaxonomySpec(folder1));
    }


    @Test(expected=ArchivistWriteException.class)
    public void testCreateFailureNested() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1.getId()));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1.getId()));

        taxonomyService.createTaxonomy(new TaxonomySpec(folder1));
        taxonomyService.createTaxonomy(new TaxonomySpec(folder3));
    }

    @Test
    public void testGet() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.createTaxonomy(new TaxonomySpec(folder1));
        Taxonomy tax2 = taxonomyService.getTaxonomy(tax1.getTaxonomyId());
        assertEquals(tax1, tax2);
    }

    @Test
    public void testGetByFolder() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.createTaxonomy(new TaxonomySpec(folder1));
        Taxonomy tax2 = taxonomyService.getTaxonomy(folder1);
        assertEquals(tax1, tax2);
    }
}
