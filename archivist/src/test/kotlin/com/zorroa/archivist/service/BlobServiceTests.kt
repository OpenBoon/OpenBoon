package com.zorroa.archivist.service

import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.BlobSpec
import com.zorroa.archivist.domain.SetPermissions
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class BlobServiceTests : AbstractTest() {

    @Autowired
    internal lateinit var blobService: BlobService

    @Test
    fun testSet() {
        val blob1 = blobService.set("app", "feature", "name",
                ImmutableMap.of("foo", "bar"))
        val blob2 = blobService.get("app", "feature", "name")
        assertEquals(blob1, blob2)
    }

    @Test
    fun testSetWithSpec() {
        val p = permissionService.getPermission("zorroa::manager")
        val s = BlobSpec(ImmutableMap.of("foo", "bar"), Acl().addEntry(p, 3))

        val blob1 = blobService.set("app", "feature", "name", s)
        val blob2 = blobService.get("app", "feature", "name")
        assertEquals(blob1, blob2)

        val acl = blobService.getPermissions(blob1)
        assertEquals(1, acl.size.toLong())
        assertEquals(p.id, acl[0].permissionId)
        assertEquals(3, acl[0].access.toLong())
    }

    @Test
    fun testSetAndReset() {
        val blob1 = blobService.set("app", "feature", "name",
                ImmutableMap.of("foo", "bar"))
        val blob2 = blobService.set("app", "feature", "name",
                ImmutableMap.of("foo", "bing"))

        assertEquals(blob1, blob2)
        assertEquals("bing", (blob2.data as Map<*, *>)["foo"])
    }

    @Test
    fun testSetPermissionsWithReplace() {
        val blob = blobService.set("app", "feature", "name",
                ImmutableMap.of("foo", "bar"))

        val p = permissionService.getPermission("zorroa::manager")
        val acl1 = blobService.setPermissions(blob, SetPermissions(Acl().addEntry(p, 3), true))

        assertEquals(1, acl1.size)
        assertEquals(p.id, acl1[0].permissionId)
        assertEquals(3, acl1[0].access)

        val acl2 = blobService.getPermissions(blob)

        assertEquals(1, acl2.size)
        assertEquals(p.id, acl2[0].permissionId)
        assertEquals(3, acl2[0].access)

    }
}
