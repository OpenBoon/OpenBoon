package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.search.AssetSearch;
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
    public void testIndexWithLink() throws InterruptedException {
        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToLinks("foo", 1);

        Document asset1 = assetService.index(builder);
        assertEquals(ImmutableList.of(1),
                asset1.getAttr("links.foo"));
    }

    @Test
    public void testIndexWithPermission() throws InterruptedException {
        Permission p = userService.getPermission("group::everyone");

        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions("group::everyone", 7);

        Document asset1 = assetService.index(builder);
        assertEquals(ImmutableList.of(p.getId()),
                asset1.getAttr("permissions.read"));

        assertEquals(ImmutableList.of(p.getId()),
                asset1.getAttr("permissions.write"));

        assertEquals(ImmutableList.of(p.getId()),
                asset1.getAttr("permissions.export"));
    }

    @Test
    public void testIndexWithReadOnlyPermission() throws InterruptedException {
        Permission p = userService.getPermission("group::everyone");

        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions("group::everyone", 1);

        Document asset1 = assetService.index(builder);
        assertEquals(ImmutableList.of(p.getId()),
                asset1.getAttr("permissions.read"));

        assertNotEquals(ImmutableList.of(p.getId()),
                asset1.getAttr("permissions.write"));

        assertNotEquals(ImmutableList.of(p.getId()),
                asset1.getAttr("permissions.export"));
    }

    @Test
    public void testIndexRemovePermissions() throws InterruptedException {
        Permission p = userService.getPermission("group::everyone");

        Source builder = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions("group::everyone", 7);
        Document asset1 = assetService.index(builder);
        refreshIndex();

        Source builder2 = new Source(getTestImagePath("set01/toucan.jpg"));
        builder.addToPermissions("group::everyone", 1);
        Document asset2 = assetService.index(builder);

        // Should only end up with read.
        assertEquals(ImmutableList.of(p.getId()),
                asset2.getAttr("permissions.read"));

        assertNotEquals(ImmutableList.of(p.getId()),
                asset2.getAttr("permissions.write"));

        assertNotEquals(ImmutableList.of(p.getId()),
                asset2.getAttr("permissions.export"));

        Document asset3 = assetService.get(asset2.getId());

        // Should only end up with read.
        assertEquals(ImmutableList.of(p.getId()),
                asset3.getAttr("permissions.read"));

        assertNotEquals(ImmutableList.of(p.getId()),
                asset3.getAttr("permissions.write"));

        assertNotEquals(ImmutableList.of(p.getId()),
                asset3.getAttr("permissions.export"));
    }

    @Test
    public void testSetPermissions() throws InterruptedException {

        Permission p = userService.getPermission("user::user");
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

        PermissionSchema schema = assets.get(0).getAttr("permissions", PermissionSchema.class);
        assertTrue(schema.getRead().contains(p.getId()));
        assertFalse(schema.getWrite().contains(p.getId()));
        assertFalse(schema.getExport().contains(p.getId()));

        schema = assets.get(1).getAttr("permissions", PermissionSchema.class);
        assertTrue(schema.getRead().contains(p.getId()));
        assertFalse(schema.getWrite().contains(p.getId()));
        assertFalse(schema.getExport().contains(p.getId()));

    }
}
