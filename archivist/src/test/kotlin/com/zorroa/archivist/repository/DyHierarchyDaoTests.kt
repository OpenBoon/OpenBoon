package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableList
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals

/**
 * Created by chambers on 7/15/16.
 */
class DyHierarchyDaoTests : AbstractTest() {

    @Autowired
    internal var dyHierarchyDao: DyHierarchyDao? = null

    lateinit var dyhi: DyHierarchy
    lateinit var folder: Folder

    @Before
    fun init() {
        folder = folderService.create(FolderSpec("foo"), false)
        val spec = DyHierarchySpec()
        spec.folderId = folder.id
        spec.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                DyHierarchyLevel("source.type.raw"),
                DyHierarchyLevel("source.extension.raw"),
                DyHierarchyLevel("source.filename.raw"))
        dyhi = dyHierarchyDao!!.create(spec)
    }

    @Test
    fun testCreate() {
        assertEquals(dyhi.folderId, folder.id)
        assertEquals(4, dyhi.levels.size.toLong())
    }

    @Test
    fun testGetByFolder() {
        val d2 = dyHierarchyDao!![folder]
        assertEquals(dyhi, d2)
    }

    @Test
    fun testGet() {
        val d2 = dyHierarchyDao!!.get(dyhi.id)
        assertEquals(dyhi, d2)
    }


    @Test
    fun testDelete() {
        assertTrue(dyHierarchyDao!!.delete(dyhi.id))
    }

    @Test
    fun testUpdate() {
        val result = dyHierarchyDao!!.update(dyhi.id,
                dyhi.setLevels(ImmutableList.of(DyHierarchyLevel("source.filename.raw"))))
        assertTrue(result)
        val d2 = dyHierarchyDao!!.refresh(dyhi)
        assertEquals(1, d2.levels.size.toLong())
    }

    @Test
    fun testGetAll() {
        val count = dyHierarchyDao!!.count()
        var list = dyHierarchyDao!!.getAll()
        assertEquals(count, list.size.toLong())

        val (id) = folderService.create(FolderSpec("bar"), false)
        val spec = DyHierarchySpec()
        spec.folderId = id
        spec.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day))
        dyHierarchyDao!!.create(spec)
        list = dyHierarchyDao!!.getAll()
        assertEquals(count + 1, list.size.toLong())
    }


    @Test
    fun testGetAllPaged() {
        val count = dyHierarchyDao!!.count()
        var list = dyHierarchyDao!!.getAll(Pager.first())
        assertEquals(count, list.size().toLong())

        val (id) = folderService.create(FolderSpec("bar"), false)
        val spec = DyHierarchySpec()
        spec.folderId = id
        spec.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day))
        dyHierarchyDao!!.create(spec)
        list = dyHierarchyDao!!.getAll(Pager.first())
        assertEquals(count + 1, list.size().toLong())
    }
}
