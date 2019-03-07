package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.Taxonomy
import com.zorroa.archivist.domain.TaxonomySpec
import com.zorroa.archivist.service.FolderService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import org.junit.Assert.*

class TaxonomyDaoTests : AbstractTest() {

    @Autowired
    lateinit var taxonomyDao: TaxonomyDao

    @Test
    fun testCreate() {
        val (id) = folderService.create(FolderSpec("foo"))
        val tax = taxonomyDao.create(TaxonomySpec().setFolderId(id))
        assertEquals(tax.folderId, id)
    }

    @Test
    fun testGet() {
        val (id) = folderService.create(FolderSpec("foo"))
        val tax1 = taxonomyDao.create(TaxonomySpec().setFolderId(id))
        val tax2 = taxonomyDao.get(tax1.taxonomyId)
        assertEquals(tax1, tax2)
    }


    @Test
    fun testGetByFolder() {
        val folder = folderService.create(FolderSpec("foo"))
        val tax1 = taxonomyDao.create(TaxonomySpec().setFolderId(folder.id))
        val tax2 = taxonomyDao!![folder]
        assertEquals(tax1, tax2)
    }
}
