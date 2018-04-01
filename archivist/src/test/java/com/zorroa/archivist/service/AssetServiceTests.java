package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.sdk.security.Groups;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by chambers on 9/1/16.
 */
public class AssetServiceTests extends AbstractTest {

    @Autowired
    CommandService commandService;

    @Before
    public void init() {
        addTestAssets("set04/standard");
        refreshIndex();
    }

    @Test
    public void testGetAsset() {
        PagedList<Document> assets = assetService.getAll(Pager.first());
        for (Document a: assets) {
            assertEquals(a.getId(),
                    assetService.get(Paths.get(a.getAttr("source.path", String.class))).getId());
        }
    }

    @Test
    public void tetDelete() {
        PagedList<Document> assets = assetService.getAll(Pager.first());
        for (Document a: assets) {
            assertTrue(assetService.delete(a.getId()));
        }
    }

    @Test
    public void testIndexWithLink() throws InterruptedException {
        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToLinks("foo", "abc123");

        Document asset1 = assetService.index(builder);
        assertEquals(ImmutableList.of(1),
                asset1.getAttr("zorroa.links.foo"));
    }

    @Test
    public void testIndexCheckOrigin() throws InterruptedException {
        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        Document asset1 = assetService.index(builder);

        logger.info("{}", Json.prettyString(asset1.getDocument()));

        assertNotNull(asset1.getAttr("zorroa.timeCreated"));
        assertNotNull(asset1.getAttr("zorroa.timeModified"));
        assertEquals(asset1.getAttr("zorroa.timeCreated", String.class),
                asset1.getAttr("zorroa.timeModified", String.class));

        refreshIndex(1000);
        Source builder2 = new Source(getTestImagePath("set01/toucan.jpg"));
        Document asset2 = assetService.index(builder2);
        assertNotEquals(asset2.getAttr("zorroa.timeCreated", String.class),
                asset2.getAttr("zorroa.timeModified", String.class));
    }

    @Test
    public void testIndexWithPermission() throws InterruptedException {
        Permission p = permissionService.getPermission(Groups.EVERYONE);

        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions(Groups.EVERYONE, 7);

        Document asset1 = assetService.index(builder);
        assertEquals(ImmutableList.of(p.getId().toString()),
                asset1.getAttr("zorroa.permissions.read"));

        assertEquals(ImmutableList.of(p.getId().toString()),
                asset1.getAttr("zorroa.permissions.write"));

        assertEquals(ImmutableList.of(p.getId().toString()),
                asset1.getAttr("zorroa.permissions.export"));
    }

    @Test
    public void testIndexWithReadOnlyPermission() throws InterruptedException {
        Permission p = permissionService.getPermission(Groups.EVERYONE);

        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions(Groups.EVERYONE, 1);

        Document asset1 = assetService.index(builder);
        assertEquals(ImmutableList.of(p.getId().toString()),
                asset1.getAttr("zorroa.permissions.read"));

        assertNotEquals(ImmutableList.of(p.getId().toString()),
                asset1.getAttr("zorroa.permissions.write"));

        assertNotEquals(ImmutableList.of(p.getId().toString()),
                asset1.getAttr("zorroa..export"));
    }

    @Test
    public void testIndexRemovePermissions() throws InterruptedException {
        Permission p = permissionService.getPermission(Groups.EVERYONE);

        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions(Groups.EVERYONE, 7);
        Document asset1 = assetService.index(builder);
        refreshIndex();

        Source builder2 = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions(Groups.EVERYONE, 1);
        Document asset2 = assetService.index(builder);

        // Should only end up with read.
        assertEquals(ImmutableList.of(p.getId().toString()),
                asset2.getAttr("zorroa.permissions.read"));

        assertNotEquals(ImmutableList.of(p.getId().toString()),
                asset2.getAttr("zorroa.permissions.write"));

        assertNotEquals(ImmutableList.of(p.getId().toString()),
                asset2.getAttr("zorroa.permissions.export"));

        Document asset3 = assetService.get(asset2.getId());

        // Should only end up with read.
        assertEquals(ImmutableList.of(p.getId().toString()),
                asset3.getAttr("zorroa.permissions.read"));

        assertNotEquals(ImmutableList.of(p.getId().toString()),
                asset3.getAttr("zorroa.permissions.write"));

        assertNotEquals(ImmutableList.of(p.getId().toString()),
                asset3.getAttr("zorroa.permissions.export"));
    }

    @Test
    public void testSetPermissions() throws InterruptedException {

        Permission p = permissionService.getPermission("user::user");
        Acl acl = new Acl();
        acl.add(new AclEntry(p.getId(), Access.Read));

        CommandSpec spec = new CommandSpec();
        spec.setType(CommandType.UpdateAssetPermissions);
        spec.setArgs(new Object[] {
                new AssetSearch(),
                acl
        });

        Command cmd = commandService.submit(spec);
        commandService.run(commandService.refresh(cmd));

        for(;;) {
            Thread.sleep(200);
            cmd = commandService.refresh(cmd);
            if (cmd.getState().equals(JobState.Finished)) {
                logger.info("Command {} finished", cmd);
                break;
            }
        }
        refreshIndex();

        PagedList<Document> assets = assetService.getAll(Pager.first());
        assertEquals(2, assets.size());

        PermissionSchema schema = assets.get(0).getAttr("zorroa.permissions", PermissionSchema.class);
        assertTrue(schema.getRead().contains(p.getId()));
        assertFalse(schema.getWrite().contains(p.getId()));
        assertFalse(schema.getExport().contains(p.getId()));

        schema = assets.get(1).getAttr("zorroa.permissions", PermissionSchema.class);
        assertTrue(schema.getRead().contains(p.getId()));
        assertFalse(schema.getWrite().contains(p.getId()));
        assertFalse(schema.getExport().contains(p.getId()));

    }
}
