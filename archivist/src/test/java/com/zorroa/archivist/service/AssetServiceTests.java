package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Access;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.AclEntry;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.search.AssetSearch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 9/1/16.
 */
public class AssetServiceTests extends AbstractTest {

    @Autowired
    AssetService assetService;

    @Before
    public void init() {
        addTestAssets("set04/standard");
        refreshIndex();
    }

    @Test
    public void testGetAsset() {
        PagedList<Asset> assets = assetService.getAll(Pager.first());
        for (Asset a: assets) {
            assertEquals(a.getId(),
                    assetService.get(Paths.get(a.getAttr("source.path", String.class))).getId());
        }
    }

    @Test
    public void testSetPermissions() throws InterruptedException {

        Permission p = userService.getPermission("user::user");
        Acl acl = new Acl();
        acl.add(new AclEntry(p.getId(), Access.Read));
        assetService.setPermissions(new AssetSearch(), acl);
        Thread.sleep(3000);

        refreshIndex();

        PagedList<Asset> assets = assetService.getAll(Pager.first());
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
