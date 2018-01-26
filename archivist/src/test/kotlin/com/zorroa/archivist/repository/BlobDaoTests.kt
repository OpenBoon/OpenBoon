package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlobDaoTests : AbstractTest() {

    @Autowired
    private lateinit var blobDao : BlobDao

    @Autowired
    private lateinit var permissionDao : PermissionDao

    lateinit var blob: Blob

    @Before
    fun init() {
        blob = blobDao.create("app", "feature", "name",
                ImmutableMap.of("foo", "bar"))
    }

    @Test
    fun testCreate() {
        assertEquals("app", blob.getApp())
        assertEquals("feature", blob.getFeature())
        assertEquals("name", blob.getName())
        assertEquals("bar", (blob.getData() as Map<*, *>)["foo"])
    }

    @Test
    fun testUpdate() {
        assertTrue(blobDao.update(blob, ImmutableMap.of("shizzle", "mcnizzle")))
    }

    @Test
    fun testDelete() {
        assertTrue(blobDao.delete(blob))
        assertFalse(blobDao.delete(blob))
    }

    @Test(expected = DuplicateKeyException::class)
    fun testCreateDuplicate() {
        blobDao.create("app", "feature", "name",
                ImmutableMap.of("foo", "bar"))
    }

    @Test
    fun testGetAll() {
        var all = blobDao.getAll("app", "feature")
        assertEquals(1, all.size.toLong())

        blobDao.create("app", "feature", "name2",
                ImmutableMap.of("foo", "bar"))
        all = blobDao.getAll("app", "feature")
        assertEquals(2, all.size.toLong())
    }

    @Test
    fun testGetId() {
        val id = blobDao.getId("app", "feature", "name", Access.Read)
        assertEquals(id.getBlobId().toLong(), blob.getBlobId().toLong())
    }

    @Test
    fun testReplacePermissions() {
        val id = blobDao.getId("app", "feature", "name", Access.Read)
        val _acl = Acl()
        _acl.addEntry(7, 7)
        blobDao.setPermissions(id, SetPermissions().apply { acl=_acl; replace=true })
        assertEquals(1, blobDao.getPermissions(id).size.toLong())
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testCheckPermissions() {
        var id = blobDao.getId("app", "feature", "name", Access.Write)
        val test = permissionDao.create(
                PermissionSpec().apply{description="foo"; name="test"; type="test"}, false)

        val _acl = Acl()
        _acl.addEntry(test, 7)

        blobDao.setPermissions(id, SetPermissions().apply { acl=_acl; replace=true })

        authenticate("user")
        id = blobDao.getId("app", "feature", "name", Access.Write)
    }

    @Test(expected = DuplicateKeyException::class)
    fun testReplacePermissionsWithDuplicate() {
        val id = blobDao.getId("app", "feature", "name", Access.Read)
        val _acl = Acl()
        _acl.addEntry(7, 7)
        _acl.addEntry(7, 7)
        blobDao.setPermissions(id, SetPermissions().apply { acl=_acl; replace=true })
    }

    @Test
    fun testUpdatePermissions() {
        val id = blobDao.getId("app", "feature", "name", Access.Read)
        var _acl = Acl()
        _acl.addEntry(7, 7)
        blobDao.setPermissions(id, SetPermissions().apply { acl=_acl; replace=false })
        assertEquals(1, blobDao.getPermissions(id).size)

        // add a new permission
        val p = permissionDao.get("group::administrator")
        _acl = Acl()
        _acl.addEntry(p, 7)
        blobDao.setPermissions(id, SetPermissions().apply { acl=_acl; replace=false })
        assertEquals(2, blobDao.getPermissions(id).size.toLong())

        // remove permission
        _acl = Acl()
        _acl.addEntry(p, 0)
        blobDao.setPermissions(id, SetPermissions().apply { acl=_acl; replace=false })
        assertEquals(1, blobDao.getPermissions(id).size.toLong())
    }
}
