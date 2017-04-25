package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 4/25/17.
 */
public class FilterServiceTests extends AbstractTest {

    @Autowired
    FilterService filterSevice;

    Asset asset;
    Filter filter;

    @Before
    public void init() {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        asset = assetService.index(source);
        refreshIndex();

        FilterSpec spec = new FilterSpec();
        spec.setAcl(new Acl().addEntry(
                userService.getPermission("group::share"), Access.Read));
        spec.setSearch(new AssetSearch(new AssetFilter().addToTerms("origin.service", "local")));
        spec.setDescription("share");
        filter = filterSevice.create(spec);
    }

    @Test
    public void testCreate() {
        assertEquals(true, filter.isEnabled());
        assertEquals("share", filter.getDescription());
        assertEquals("filter_" + filter.getId(), filter.getName());
        assertTrue(filter.getSearch().getFilter().getTerms().containsKey("origin.service"));
        assertTrue(filter.getAcl().hasAccess( userService.getPermission("group::share"), Access.Read));
    }

    @Test
    public void testGetMatchedAcls() {
        Acl acl = filterSevice.getMatchedAcls(asset);
        assertEquals(1, acl.size());
        assertTrue(filter.getAcl().hasAccess( userService.getPermission("group::share"), Access.Read));
    }

    @Test
    public void testPermissionSchemaApplied() {
        // Reindex after the filter is created.
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Asset asset = assetService.index(source);

        int userPerm = SecurityUtils.getUser().getPermissionId();
        Permission p = userService.getPermission("group::share");

        PermissionSchema perms = asset.getAttr("permissions", PermissionSchema.class);
        assertTrue(perms.getRead().contains(userPerm));
        assertTrue(perms.getWrite().contains(userPerm));
        assertTrue(perms.getExport().contains(userPerm));
        assertTrue(perms.getRead().contains(p.getId()));
    }

}
