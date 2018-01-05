package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlobDaoTests extends AbstractTest {

    @Autowired
    private BlobDao blobDao;

    @Autowired
    private PermissionDao permissionDao;

    Blob blob;

    @Before
    public void init() {
        blob = blobDao.create("app", "feature", "name",
                ImmutableMap.of("foo", "bar"));
    }

    @Test
    public void testCreate() {
        assertEquals("app", blob.getApp());
        assertEquals("feature", blob.getFeature());
        assertEquals("name", blob.getName());
        assertEquals("bar", ((Map)blob.getData()).get("foo"));
    }

    @Test
    public void testUpdate() {
        assertTrue(blobDao.update(blob, ImmutableMap.of("shizzle", "mcnizzle")));
    }

    @Test(expected=DuplicateKeyException.class)
    public void testCreateDuplicate() {
        blobDao.create("app", "feature", "name",
                ImmutableMap.of("foo", "bar"));
    }

    @Test
    public void testGetAll() {
        List<Blob> all = blobDao.getAll("app", "feature");
        assertEquals(1, all.size());

        blobDao.create("app", "feature", "name2",
                ImmutableMap.of("foo", "bar"));
        all = blobDao.getAll("app", "feature");
        assertEquals(2, all.size());
    }

    @Test
    public void testGetId() {
        BlobId id = blobDao.getId("app", "feature", "name", Access.Read);
        assertEquals(id.getBlobId(), blob.getBlobId());
    }

    @Test
    public void testReplacePermissions() {
        BlobId id = blobDao.getId("app", "feature", "name", Access.Read);
        Acl acl = new Acl();
        acl.addEntry(7, 7);
        blobDao.setPermissions(id, new SetPermissions().setAcl(acl).setReplace(true));
        assertEquals(1, blobDao.getPermissions(id).size());
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void testCheckPermissions() {
        BlobId id = blobDao.getId("app", "feature", "name", Access.Write);
        Permission test = permissionDao.create(new PermissionSpec()
                .setDescription("foo")
                .setType("test")
                .setName("test"), false);

        Acl acl = new Acl();
        acl.addEntry(test, 7);
        blobDao.setPermissions(id, new SetPermissions().setAcl(acl).setReplace(true));

        authenticate("user");
        id = blobDao.getId("app", "feature", "name", Access.Write);
    }

    @Test(expected = DuplicateKeyException.class)
    public void testReplacePermissionsWithDuplicate() {
        BlobId id = blobDao.getId("app", "feature", "name", Access.Read);
        Acl acl = new Acl();
        acl.addEntry(7, 7);
        acl.addEntry(7, 7);
        blobDao.setPermissions(id, new SetPermissions().setAcl(acl).setReplace(true));
    }

    @Test
    public void testUpdatePermissions() {
        BlobId id = blobDao.getId("app", "feature", "name", Access.Read);
        Acl acl = new Acl();
        acl.addEntry(7, 7);
        blobDao.setPermissions(id, new SetPermissions().setAcl(acl).setReplace(false));
        assertEquals(1, blobDao.getPermissions(id).size());

        // add a new permission
        Permission p = permissionDao.get("group::administrator");
        acl = new Acl();
        acl.addEntry(p, 7);
        blobDao.setPermissions(id, new SetPermissions().setAcl(acl).setReplace(false));
        assertEquals(2, blobDao.getPermissions(id).size());

        // remove permission
        acl = new Acl();
        acl.addEntry(p, 0);
        blobDao.setPermissions(id, new SetPermissions().setAcl(acl).setReplace(false));
        assertEquals(1, blobDao.getPermissions(id).size());
    }
}
