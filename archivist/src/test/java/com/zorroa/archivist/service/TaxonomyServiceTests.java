package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
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
    public void testCreateAndRun() throws InterruptedException {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1));

        Source d = new Source();
        d.setId("abc123");
        assetService.index(d);
        refreshIndex();

        folderService.addAssets(folder4, Lists.newArrayList(d.getId()));
        refreshIndex();
        taxonomyService.create(new TaxonomySpec(folder1));
        refreshIndex();

        Document doc = new Document(
                searchService.search(new AssetSearch()).getHits().getHits()[0].getSourceAsMap());
        assertEquals(ImmutableList.of("federation", "ships"), doc.getAttr("zorroa.taxonomy",
                new TypeReference<List<TaxonomySchema>>() {
                }).get(0).getKeywords());

        AssetSearch search = new AssetSearch("ships");
        assertEquals(1, searchService.search(search).getHits().getTotalHits());

    }

    @Test(expected=ArchivistWriteException.class)
    public void testCreateFailureDuplicate() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1));

        taxonomyService.create(new TaxonomySpec(folder1));
        taxonomyService.create(new TaxonomySpec(folder1));
    }


    @Test(expected=ArchivistWriteException.class)
    public void testCreateFailureNested() {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1));

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
        folder1 = folderService.get(folder1.getId());

        Source d = new Source();
        d.setId("abc123");
        d.setAttr("foo.keywords", "ships");
        assetService.index(d);
        refreshIndex();

        folderService.addAssets(folder1, Lists.newArrayList(d.getId()));
        refreshIndex();
        taxonomyService.tagTaxonomy(tax1, folder1,true);
        refreshIndex();

        Map<String, Long> result = taxonomyService.untagTaxonomy(tax1, 1000);
        assertEquals(1, result.get("assetCount").longValue());
        assertEquals(0, result.get("errorCount").longValue());
        refreshIndex();

        Document a = assetService.get(d.getId());
        assertEquals(0, a.getAttr("zorroa.taxonomy", List.class).size());
    }

    @Test
    public void testUntagTaxonomyFolders() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        folder1 = folderService.get(folder1.getId());

        Source d = new Source();
        d.setId("abc123");
        assetService.index(d);
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());

        folderService.addAssets(folder1, Lists.newArrayList(d.getId()));
        refreshIndex();
        taxonomyService.tagTaxonomy(tax1, folder1, false);
        refreshIndex();

        fieldService.invalidateFields();

        assertEquals(1, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());

        taxonomyService.untagTaxonomyFolders(tax1, folder1,
                ImmutableList.of(d.getId()));
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());
    }

    @Test
    public void testDeleteTaxonomy() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        folder1 = folderService.get(folder1.getId());

        Source d = new Source();
        d.setId("abc123");
        assetService.index(d);
        refreshIndex();

        folderService.addAssets(folder1, Lists.newArrayList(d.getId()));
        refreshIndex();
        taxonomyService.tagTaxonomy(tax1, folder1, false);
        refreshIndex();
        assertEquals(1, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());

        assertTrue(taxonomyService.delete(tax1, true));
        assertFalse(taxonomyService.delete(tax1, true));
        assertFalse(folderService.get(folder1.getId()).getTaxonomyRoot());

        refreshIndex();
        assertEquals(0, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());
    }

    @Test
    public void testDeleteRootTaxonomyFolder() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        folder1 = folderService.get(folder1.getId());

        Source d = new Source();
        d.setId("abc123");
        assetService.index(d);
        refreshIndex();

        folderService.addAssets(folder1, Lists.newArrayList(d.getId()));
        fieldService.invalidateFields();
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());
        assertTrue(fieldService.getFields("asset").get(
                "string").contains("zorroa.taxonomy.keywords"));

        folderService.trash(folder1);

        refreshIndex();
        assertEquals(0, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());
    }
}
