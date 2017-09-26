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
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;
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
    public void testCreateAndRun() throws InterruptedException {

        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Folder folder2 = folderService.create(new FolderSpec("borg", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("klingon", folder1.getId()));
        Folder folder4 = folderService.create(new FolderSpec("federation", folder1.getId()));

        Source d = new Source();
        d.setId("abc123");
        assetService.index(d);
        refreshIndex();

        folderService.addAssets(folder4, Lists.newArrayList(d.getId()));
        Taxonomy t = taxonomyService.create(new TaxonomySpec(folder1));

        Thread.sleep(2000);

        Document doc = new Document(
                searchService.search(new AssetSearch()).getHits().getHits()[0].getSource());
        assertEquals(ImmutableList.of("federation", "ships"),
                doc.getAttr(String.format("zorroa.taxonomy.tax%d.keywords", t.getTaxonomyId())));
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
        assetService.index(d);
        refreshIndex();
        /**
         * We can't index an asset with zorroa namespace, but for now we can update
         * it to make these test work.  In the future we might need to override cleaning
         * the asset so its easier to set values fo testing.
         */
        assetService.update("abc123", ImmutableMap.of(field +".timestamp", 0));
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
        assertTrue(folderService.get(folder1.getId()).isTaxonomyRoot());

        String field ="zorroa.taxonomy.tax" + tax1.getTaxonomyId();

        Source d = new Source();
        d.setId("abc123");
        d.setAttr(field,
                ImmutableMap.of("timestamp", System.currentTimeMillis()));
        assetService.index(d);
        refreshIndex();

        assertTrue(taxonomyService.delete(tax1, true));
        assertFalse(taxonomyService.delete(tax1, true));
        assertFalse(folderService.get(folder1.getId()).isTaxonomyRoot());
    }

    @Test
    public void testDeleteTaxonomyFolder() {
        Folder folder1 = folderService.create(new FolderSpec("ships"));
        Taxonomy tax1 = taxonomyService.create(new TaxonomySpec(folder1));
        folder1 = folderService.get(folder1.getId());

        Source d = new Source();
        d.setId("abc123");
        assetService.index(d);
        refreshIndex();

        // stuff should get untagged
        folderService.addAssets(folder1, Lists.newArrayList(d.getId()));
        assertTrue(searchService.getFields("asset").get(
                "string").contains(tax1.getRootField() + ".keywords"));

        folderService.trash(folder1);
        assertEquals(0, searchService.search(
                new AssetSearch("ships")).getHits().getTotalHits());
        assertFalse(searchService.getFields("asset").get(
                "string").contains(tax1.getRootField() + ".keywords"));
    }
}
