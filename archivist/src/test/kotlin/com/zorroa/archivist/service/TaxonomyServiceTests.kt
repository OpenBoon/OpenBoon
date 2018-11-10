package com.zorroa.archivist.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableList
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.TaxonomyService
import com.zorroa.common.domain.ArchivistWriteException
import org.assertj.core.util.Lists
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import org.junit.Assert.*

/**
 * Created by chambers on 6/19/17.
 */
class TaxonomyServiceTests : AbstractTest() {

    @Autowired
    lateinit var taxonomyService: TaxonomyService

    @Test
    @Throws(InterruptedException::class)
    fun testCreateAndRun() {

        val folder1 = folderService.create(FolderSpec("ships"))
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search1, taxonomyRoot, attrs) = folderService.create(FolderSpec("borg", folder1))
        val (id1, name1, parentId1, organizationId1, dyhiId1, user1, timeCreated1, timeModified1, recursive1, dyhiRoot1, dyhiField1, childCount1, acl1, search2, taxonomyRoot1, attrs1) = folderService.create(FolderSpec("klingon", folder1))
        val folder4 = folderService.create(FolderSpec("federation", folder1))

        val d = Source()
        d.id = "abc123"
        indexService.index(d)
        refreshIndex()

        folderService.addAssets(folder4, Lists.newArrayList(d.id))
        refreshIndex()
        taxonomyService.create(TaxonomySpec(folder1))
        refreshIndex()

        val doc = Document(
                searchService.search(AssetSearch()).hits.hits[0].sourceAsMap)
        assertEquals(ImmutableList.of("federation", "ships"), doc.getAttr("system.taxonomy",
                object : TypeReference<List<TaxonomySchema>>() {

                })[0].keywords)

        val search = AssetSearch("ships")
        assertEquals(1, searchService.search(search).hits.getTotalHits())

    }

    @Test(expected = ArchivistWriteException::class)
    fun testCreateFailureDuplicate() {

        val folder1 = folderService.create(FolderSpec("ships"))
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderService.create(FolderSpec("borg", folder1))
        val (id1, name1, parentId1, organizationId1, dyhiId1, user1, timeCreated1, timeModified1, recursive1, dyhiRoot1, dyhiField1, childCount1, acl1, search1, taxonomyRoot1, attrs1) = folderService.create(FolderSpec("klingon", folder1))
        val (id2, name2, parentId2, organizationId2, dyhiId2, user2, timeCreated2, timeModified2, recursive2, dyhiRoot2, dyhiField2, childCount2, acl2, search2, taxonomyRoot2, attrs2) = folderService.create(FolderSpec("federation", folder1))

        taxonomyService.create(TaxonomySpec(folder1))
        taxonomyService.create(TaxonomySpec(folder1))
    }


    @Test(expected = ArchivistWriteException::class)
    fun testCreateFailureNested() {

        val folder1 = folderService.create(FolderSpec("ships"))
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderService.create(FolderSpec("borg", folder1))
        val folder3 = folderService.create(FolderSpec("klingon", folder1))
        val (id1, name1, parentId1, organizationId1, dyhiId1, user1, timeCreated1, timeModified1, recursive1, dyhiRoot1, dyhiField1, childCount1, acl1, search1, taxonomyRoot1, attrs1) = folderService.create(FolderSpec("federation", folder1))

        taxonomyService.create(TaxonomySpec(folder1))
        taxonomyService.create(TaxonomySpec(folder3))
    }

    @Test
    fun testGet() {
        val folder1 = folderService.create(FolderSpec("ships"))
        val tax1 = taxonomyService.create(TaxonomySpec(folder1))
        val tax2 = taxonomyService.get(tax1.taxonomyId)
        assertEquals(tax1, tax2)
    }

    @Test
    fun testGetByFolder() {
        val folder1 = folderService.create(FolderSpec("ships"))
        val tax1 = taxonomyService.create(TaxonomySpec(folder1))
        val tax2 = taxonomyService.get(folder1)
        assertEquals(tax1, tax2)
    }

    @Test
    fun testUntagTaxonomy() {
        var folder1 = folderService.create(FolderSpec("ships"))
        val tax1 = taxonomyService.create(TaxonomySpec(folder1))
        folder1 = folderService.get(folder1.id)

        val d = Source()
        d.id = "abc123"
        d.setAttr("foo.keywords", "ships")
        indexService.index(d)
        refreshIndex()

        folderService.addAssets(folder1, Lists.newArrayList(d.id))
        refreshIndex()
        taxonomyService.tagTaxonomy(tax1, folder1, true)
        refreshIndex()

        val result = taxonomyService.untagTaxonomy(tax1, 1000)
        assertEquals(1, result["assetCount"]!!.toLong())
        assertEquals(0, result["errorCount"]!!.toLong())
        refreshIndex()

        val a = indexService.get(d.id)
        assertEquals(0, a.getAttr("system.taxonomy", List::class.java).size.toLong())
    }

    @Test
    fun testUntagTaxonomyFolders() {
        var folder1 = folderService.create(FolderSpec("ships"))
        val tax1 = taxonomyService.create(TaxonomySpec(folder1))
        folder1 = folderService.get(folder1.id)

        val d = Source()
        d.id = "abc123"
        indexService.index(d)
        refreshIndex()

        assertEquals(0, searchService.search(
                AssetSearch("ships")).hits.getTotalHits())

        folderService.addAssets(folder1, Lists.newArrayList(d.id))
        refreshIndex()
        taxonomyService.tagTaxonomy(tax1, folder1, false)
        refreshIndex()

        fieldService.invalidateFields()

        assertEquals(1, searchService.search(
                AssetSearch("ships")).hits.getTotalHits())

        taxonomyService.untagTaxonomyFolders(tax1, folder1,
                ImmutableList.of(d.id))
        refreshIndex()

        assertEquals(0, searchService.search(
                AssetSearch("ships")).hits.getTotalHits())
    }

    @Test
    fun testDeleteTaxonomy() {
        var folder1 = folderService.create(FolderSpec("ships"))
        val tax1 = taxonomyService.create(TaxonomySpec(folder1))
        folder1 = folderService.get(folder1.id)

        val d = Source()
        d.id = "abc123"
        indexService.index(d)
        refreshIndex()

        folderService.addAssets(folder1, Lists.newArrayList(d.id))
        refreshIndex()
        taxonomyService.tagTaxonomy(tax1, folder1, false)
        refreshIndex()
        assertEquals(1, searchService.search(
                AssetSearch("ships")).hits.getTotalHits())

        assertTrue(taxonomyService.delete(tax1, true))
        assertFalse(taxonomyService.delete(tax1, true))
        assertFalse(folderService.get(folder1.id).taxonomyRoot)

        refreshIndex()
        assertEquals(0, searchService.search(
                AssetSearch("ships")).hits.getTotalHits())
    }

    @Test
    fun testDeleteRootTaxonomyFolder() {
        var folder1 = folderService.create(FolderSpec("ships"))
        val tax1 = taxonomyService.create(TaxonomySpec(folder1))
        folder1 = folderService.get(folder1.id)

        val d = Source()
        d.id = "abc123"
        indexService.index(d)
        refreshIndex()

        folderService.addAssets(folder1, Lists.newArrayList(d.id))
        fieldService.invalidateFields()
        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("ships")).hits.getTotalHits())
        assertTrue(fieldService.getFields("asset")["string"]!!.contains("system.taxonomy.keywords"))

        folderService.trash(folder1)

        refreshIndex()
        assertEquals(0, searchService.search(
                AssetSearch("ships")).hits.getTotalHits())
    }
}
