package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.TrashedFolder
import com.zorroa.archivist.security.*
import com.zorroa.archivist.service.FolderService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import org.junit.Assert.assertEquals

/**
 * Created by chambers on 12/5/16.
 */
class TrashFolderDaoTests : AbstractTest() {

    @Autowired
    lateinit var trashFolderDao: TrashFolderDao

    @Test
    fun testCreate() {
        val folder1 = folderService.create(FolderSpec("test1"))
        trashFolderDao.create(folder1, "a", true, 1)

        val count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM folder_trash WHERE str_opid=?", Int::class.java, "a")
        assertEquals(count.toLong(), 1)
    }

    @Test
    fun testGetAllByOp() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder2 = folderService.create(FolderSpec("test2"))

        trashFolderDao.create(folder1, "a", true, 1)
        trashFolderDao.create(folder2, "a", false, 1)

        val folders = trashFolderDao.getAll("a")
        assertEquals(2, folders.size.toLong())
    }

    @Test
    fun testGetAllByOpWithChildren() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder2 = folderService.create(FolderSpec("test2", folder1))

        trashFolderDao.create(folder1, "a", true, 0)
        trashFolderDao.create(folder2, "a", false, 1)

        val folders = trashFolderDao.getAll("a")
        assertEquals(2, folders.size.toLong())
    }

    @Test
    fun testGetAllByParent() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder2 = folderService.create(FolderSpec("test2"))
        trashFolderDao.create(folder1, "a", true, 1)
        trashFolderDao.create(folder2, "a", false, 2)

        var folders = trashFolderDao.getAll(folderService.getRoot(),
                getUserId())
        assertEquals(1, folders.size.toLong())

        folders = trashFolderDao.getAll(folder1, getUserId())
        assertEquals(0, folders.size.toLong())

        folders = trashFolderDao.getAll(folder2, getUserId())
        assertEquals(0, folders.size.toLong())
    }

    @Test
    fun testGetAllByUser() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder2 = folderService.create(FolderSpec("test2"))
        trashFolderDao.create(folder1, "a", true, 1)
        trashFolderDao.create(folder2, "a", false, 2)

        val folders = trashFolderDao.getAll(getUserId())
        assertEquals(1, folders.size.toLong())
    }


    @Test
    fun testCount() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder2 = folderService.create(FolderSpec("test2"))
        trashFolderDao.create(folder1, "a", true, 1)
        trashFolderDao.create(folder2, "a", false, 2)

        val count = trashFolderDao.count(getUserId())
        assertEquals(1, count.toLong())
    }
}
