package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.sdk.security.Groups
import com.zorroa.sdk.domain.Access
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.Source
import com.zorroa.sdk.schema.PermissionSchema
import com.zorroa.sdk.search.AssetSearch
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Paths

/**
 * Created by chambers on 9/1/16.
 */
class IndexServiceTests : AbstractTest() {

    @Autowired
    internal var commandService: CommandService? = null

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun testGetAsset() {
        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertEquals(a.id,
                    indexService.get(Paths.get(a.getAttr("source.path", String::class.java))).id)
        }
    }

    @Test
    fun tetDelete() {
        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertTrue(indexService.delete(a.id))
        }
    }

    @Test
    fun testIndexWithLink() {
        val builder = Source(getTestImagePath("set01/toucan.jpg"))
        builder.addToLinks("foo", "abc123")

        val asset1 = indexService.index(builder)
        assertEquals(ImmutableList.of("abc123"),
                asset1.getAttr("zorroa.links.foo"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIndexCheckOrigin() {
        val builder = Source(getTestImagePath("set01/toucan.jpg"))
        val asset1 = indexService.index(builder)

        assertNotNull(asset1.getAttr("zorroa.timeCreated"))
        assertNotNull(asset1.getAttr("zorroa.timeModified"))
        assertEquals(asset1.getAttr("zorroa.timeCreated", String::class.java),
                asset1.getAttr("zorroa.timeModified", String::class.java))

        refreshIndex()
        Thread.sleep(1000)
        val builder2 = Source(getTestImagePath("set01/toucan.jpg"))
        val asset2 = indexService.index(builder2)

        refreshIndex()
        assertNotEquals(asset2.getAttr("zorroa.timeCreated", String::class.java),
                asset2.getAttr("zorroa.timeModified", String::class.java))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIndexWithPermission() {
        val p = permissionService.getPermission(Groups.EVERYONE)

        val builder = Source(getTestImagePath("set01/toucan.jpg"))
        builder.addToPermissions(Groups.EVERYONE, 7)

        val asset1 = indexService.index(builder)
        assertEquals(ImmutableList.of(p.id.toString()),
                asset1.getAttr("zorroa.permissions.read"))

        assertEquals(ImmutableList.of(p.id.toString()),
                asset1.getAttr("zorroa.permissions.write"))

        assertEquals(ImmutableList.of(p.id.toString()),
                asset1.getAttr("zorroa.permissions.export"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIndexWithReadOnlyPermission() {
        val p = permissionService.getPermission(Groups.EVERYONE)

        val builder = Source(getTestImagePath("set01/toucan.jpg"))
        builder.addToPermissions(Groups.EVERYONE, 1)

        val asset1 = indexService.index(builder)
        assertEquals(ImmutableList.of(p.id.toString()),
                asset1.getAttr("zorroa.permissions.read"))

        assertNotEquals(ImmutableList.of(p.id.toString()),
                asset1.getAttr("zorroa.permissions.write"))

        assertNotEquals(ImmutableList.of(p.id.toString()),
                asset1.getAttr("zorroa..export"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIndexRemovePermissions() {
        val p = permissionService.getPermission(Groups.EVERYONE)

        val builder = Source(getTestImagePath("set01/toucan.jpg"))
        builder.addToPermissions(Groups.EVERYONE, 7)
        val asset1 = indexService.index(builder)
        refreshIndex()

        val builder2 = Source(getTestImagePath("set01/toucan.jpg"))
        builder.addToPermissions(Groups.EVERYONE, 1)
        val asset2 = indexService.index(builder)

        // Should only end up with read.
        assertEquals(ImmutableList.of(p.id.toString()),
                asset2.getAttr("zorroa.permissions.read"))

        assertNotEquals(ImmutableList.of(p.id.toString()),
                asset2.getAttr("zorroa.permissions.write"))

        assertNotEquals(ImmutableList.of(p.id.toString()),
                asset2.getAttr("zorroa.permissions.export"))

        val asset3 = indexService.get(asset2.id)

        // Should only end up with read.
        assertEquals(ImmutableList.of(p.id.toString()),
                asset3.getAttr("zorroa.permissions.read"))

        assertNotEquals(ImmutableList.of(p.id.toString()),
                asset3.getAttr("zorroa.permissions.write"))

        assertNotEquals(ImmutableList.of(p.id.toString()),
                asset3.getAttr("zorroa.permissions.export"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSetPermissions() {

        val p = permissionService.getPermission("user::user")
        val acl = Acl()
        acl.add(AclEntry(p.id, Access.Read))

        val spec = CommandSpec()
        spec.type = CommandType.UpdateAssetPermissions
        spec.args = arrayOf(AssetSearch(), acl)

        var cmd = commandService!!.submit(spec)
        commandService!!.run(commandService!!.refresh(cmd))

        while (true) {
            Thread.sleep(200)
            cmd = commandService!!.refresh(cmd)
            if (cmd.state == JobState.Finished) {
                break
            }
        }
        refreshIndex()

        val assets = indexService.getAll(Pager.first())
        assertEquals(2, assets.size().toLong())

        var schema = assets.get(0).getAttr("zorroa.permissions", PermissionSchema::class.java)
        assertTrue(schema.read.contains(p.id))
        assertFalse(schema.write.contains(p.id))
        assertFalse(schema.export.contains(p.id))

        schema = assets.get(1).getAttr("zorroa.permissions", PermissionSchema::class.java)
        assertTrue(schema.read.contains(p.id))
        assertFalse(schema.write.contains(p.id))
        assertFalse(schema.export.contains(p.id))

    }
}
