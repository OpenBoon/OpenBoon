package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.security.Groups;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Access;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.zorroa.archivist.domain.FolderKt.getRootFolderId;
import static org.junit.Assert.*;

public class FolderServiceTests extends AbstractTest {

    @Autowired
    DyHierarchyService dyhiService;

    @Autowired
    FolderDao folderDao;

    @Autowired
    TaxonomyService taxonomyService;

    @Before
    public void init() {
        addTestAssets("set04/standard");
        refreshIndex();
    }

    @Test
    public void testSetAssets() {

        List<UUID> folders = Lists.newArrayList();
        for (int i=0; i<10; i++) {
            FolderSpec builder = new FolderSpec("Folder" + i);
            Folder folder = folderService.create(builder);
            folders.add(folder.getId());
        }

        List<Document> assets = indexService.getAll(Pager.first(1)).getList();
        assertEquals(1, assets.size());
        Document doc = assets.get(0);

        folderService.setFoldersForAsset(doc.getId(), folders);
        refreshIndex();

        doc = indexService.get(doc.getId());
        assertEquals(10, doc.getAttr("zorroa.links.folder", List.class).size());
    }

    @Test
    public void testAddAssetToFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);

        Map<String, List<Object>> results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());
    }

    @Test
    public void testAddDuplicateAssetsToFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);

        folderService.addAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        refreshIndex();

        folderService.addAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        refreshIndex();

        PagedList<Document> assets = indexService.getAll(Pager.first());
        for (Document a: assets) {
            assertEquals(1, ((List) a.getAttr("zorroa.links.folder")).size());
        }
    }

    @Test
    public void testRemoveAssetFromFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);

        Map<String, List<Object>> results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());

        results = folderService.removeAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());
    }

    @Test
    public void testAddAssetToTaxonomyFolder() {

        FolderSpec builder = new FolderSpec("bilbo");
        Folder folder = folderService.create(builder);
        taxonomyService.create(new TaxonomySpec(folder));
        folder = folderService.get(folder.getId());

        Map<String, List<Object>> results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        refreshIndex(2000);

        assertEquals(2, searchService.count(new AssetSearch().setQuery("bilbo")));
    }

    @Test
    public void testRemoveAssetFromTaxonomyFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        taxonomyService.create(new TaxonomySpec(folder));
        folder = folderService.get(folder.getId());

        Map<String, List<Object>> results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        refreshIndex();

        assertEquals(2, searchService.search(new AssetSearch(
                new AssetFilter().addToTerms("zorroa.links.folder", folder.getId()))).getHits().getTotalHits());
        fieldService.invalidateFields();
        refreshIndex();
        assertEquals(2, searchService.search(new AssetSearch("Folder")).getHits().getTotalHits());

        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());

        results = folderService.removeAssets(folder, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());
        refreshIndex();

        assertEquals(0, searchService.search(new AssetSearch(
                new AssetFilter().addToTerms("zorroa.links.folder", folder.getId()))).getHits().getTotalHits());
        assertEquals(0, searchService.search(new AssetSearch("Folder")).getHits().getTotalHits());

    }

    @Test
    public void testCountAssetSmartFolder() {
        FolderSpec builder = new FolderSpec("Folder");
        builder.setSearch(new AssetSearch("jpg"));

        Folder folder = folderService.create(builder);
        assertEquals(2, searchService.count(folderService.get(folder.getId())));

        FolderSpec builder2 = new FolderSpec("Folder2");
        builder2.setSearch(new AssetSearch("wdsdsdsdsds"));
        Folder folder2 = folderService.create(builder2);

        assertEquals(0, searchService.count(folderService.get(folder2.getId())));
    }

    @Test
    public void testSetAcl() {
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        folderService.setAcl(folder, new Acl().addEntry(
                permissionService.getPermission(Groups.MANAGER),
                Access.Read, Access.Write, Access.Export), false, false);
        folderService.get(folder.getId());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetAclFailure() {
        authenticate("librarian");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        /**
         * Since we have all permissions now, this should fail because we
         * are taking away write/export permissions from ourself.
         */
        folderService.setAcl(folder, new Acl().addEntry(
                permissionService.getPermission(Groups.LIBRARIAN),
                Access.Read), false, false);
        folderService.get(folder.getId());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetAclFailureDoesntHavePermission() {
        authenticate("librarian");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        /**
         * Since we have all permissions now, this should fail because we
         * are taking away write/export permissions from ourself.
         */
        folderService.setAcl(folder, new Acl().addEntry(
                permissionService.getPermission(Groups.ADMIN),
                Access.Read), false, false);
        folderService.get(folder.getId());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testGetFolderWithoutAcl() {
        authenticate("librarian");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        // use the DAO so don't fail the remove access from self check.
        folderDao.setAcl(folder.getId(), new Acl().addEntry(
                permissionService.getPermission(Groups.ADMIN), Access.Read));
        folderService.invalidate(folder);
        folderService.get(folder.getId());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testCreateWithReadAcl() {
        authenticate("librarian");
        FolderSpec builder = new FolderSpec("Folder", folderService.get("/Library"));
        Folder folder = folderService.create(builder);
        folderDao.setAcl(folder.getId(),
                new Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Read));
        folderService.invalidate(folder);
        folderService.get(folder.getId());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testAddAssetsWithWriteAcl() {
        authenticate("librarian");
        FolderSpec builder = new FolderSpec("Folder", folderService.get("/Library"));
        Folder folder = folderService.create(builder);
        Acl acl = new Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Write);
        folderService.setAcl(folder, acl, false, false);
        folderService.addAssets(folderService.get(folder), indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testDeleteFolderWithWriteAcl() {
        authenticate("librarian");
        FolderSpec builder = new FolderSpec("Folder", folderService.get("/Library"));
        Folder folder = folderService.create(builder);
        Acl acl = new Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Write);
        folderService.setAcl(folder, acl, false, false);
        folderService.delete(folderService.get(folder));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testUpdateFolderWithWriteAcl() {
        authenticate("librarian");
        FolderSpec builder = new FolderSpec("Folder", folderService.get("/Library"));
        Folder folder = folderService.create(builder);
        folderDao.setAcl(folder.getId(),
                new Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Write));
        FolderUpdate up = new FolderUpdate(folder);
        up.setName("bilbo");
        folderService.update(folder.getId(), up);
    }

    @Test
    public void testRenameUserFolder() {
        authenticate("librarian");
        User user = userService.get("librarian");
        assertFalse(folderService.exists("/Users/foo"));
        assertTrue(folderService.renameUserFolder(user, "foo"));
        assertTrue(folderService.exists("/Users/foo"));
    }

    @Test
    public void testCreateAndGet() {
        FolderSpec builder = new FolderSpec("Da Kind Assets");
        Folder folder1 = folderService.create(builder);

        Folder folder2 = folderService.get(folder1.getId());
        assertEquals(folder1.getName(), folder2.getName());
    }

    @Test
    public void testDescendants() {
        Folder grandpa = folderService.create(new FolderSpec("grandpa"));
        Folder dad = folderService.create(new FolderSpec("dad", grandpa));
        Folder uncle = folderService.create(new FolderSpec("uncle", grandpa));
        folderService.create(new FolderSpec("child", dad));
        folderService.create(new FolderSpec("cousin", uncle));
        List<Folder> descendents = folderService.getAllDescendants(grandpa, false);
        assertEquals(4, descendents.size());
    }

    @Test
    public void testGetAllDescendants() {
        Folder grandpa = folderService.create(new FolderSpec("grandpa"));
        Folder dad = folderService.create(new FolderSpec("dad", grandpa));
        Folder uncle = folderService.create(new FolderSpec("uncle", grandpa));
        folderService.create(new FolderSpec("child", dad));
        folderService.create(new FolderSpec("cousin", uncle));
        assertEquals(5, folderService.getAllDescendants(Lists.newArrayList(grandpa), true, true).size());
        assertEquals(4, folderService.getAllDescendants(Lists.newArrayList(grandpa), false, true).size());

        assertEquals(5, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), true, true)).size());
        assertEquals(4, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), false, true)).size());
    }

    @Test
    public void testGetChildren() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder1a = folderService.create(new FolderSpec("test1a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test1b", folder1));
        Folder folder1c = folderService.create(new FolderSpec("test1c", folder1));

        List<Folder> children = folderService.getChildren(folder1);
        assertEquals(3, children.size());
        assertTrue(children.contains(folder1a));
        assertTrue(children.contains(folder1b));
        assertTrue(children.contains(folder1c));
        assertFalse(children.contains(folder1));
    }

    @Test
    public void testGetByPath() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder1a = folderService.create(new FolderSpec("test1a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test1b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test1c", folder1b));

        Folder folder = folderService.get("/test1/test1a/test1b/test1c");
        assertEquals(folder1b.getId(), folder.getParentId());
        assertEquals(folder1c.getName(), folder.getName());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testGetByPathFail() {
        Folder folder = folderService.get("/foo/bar/bam");
        assertEquals(null, folder);
    }

    @Test
    public void testExistsByPath() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder1a = folderService.create(new FolderSpec("test1a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test1b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test1c", folder1b));

        assertTrue(folderService.exists("/test1/test1a/test1b/test1c"));
        assertTrue(folderService.exists("/test1/test1a"));
        assertFalse(folderService.exists("/testb"));
        assertFalse(folderService.exists("/testb/test123"));
    }

    @Test
    public void testUpdate() {
        Folder folder = folderService.create(new FolderSpec("orig"));
        FolderUpdate up = new FolderUpdate(folder);
        up.setName("new");
        boolean ok = folderService.update(folder.getId(), up);
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertEquals("new", revised.getName());
    }

    @Test
    public void testUpdateWithNewParent() {

        FolderSpec fs1 = new FolderSpec("orig");
        FolderSpec fs2 = new FolderSpec("unorig");

        Folder folder1 = folderService.create(fs1);
        Folder folder2 = folderService.create(fs2);
        FolderUpdate up = new FolderUpdate(folder2);
        up.setParentId(folder1.getId());
        boolean ok = folderService.update(folder2.getId(), up);
        assertTrue(ok);

        Folder revised = folderService.get(folder2.getId());
        assertEquals(folder1.getId(), revised.getParentId());

        List<Folder> folders = folderService.getAllDescendants(Lists.newArrayList(folder1), false, false);
        assertTrue(folders.contains(folder2));

        folders = folderService.getAllDescendants(Lists.newArrayList(folder2), false, false);
        assertTrue(folders.isEmpty());
    }

    @Test
    public void testUpdateWithNewTaxonomyParent() {

        assertEquals(0, searchService.count(new AssetSearch().setQuery("bilbo")));

        FolderSpec builder1 = new FolderSpec("bilbo");
        Folder folder1 = folderService.create(builder1);
        taxonomyService.create(new TaxonomySpec(folder1));
        folder1 = folderService.get(folder1.getId());

        FolderSpec builder2 = new FolderSpec("baggins");
        Folder folder2 = folderService.create(builder2);

        Map<String, List<Object>> results = folderService.addAssets(folder2, indexService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        refreshIndex(1000);

        assertEquals(0, searchService.count(new AssetSearch().setQuery("bilbo")));

        FolderUpdate update = new FolderUpdate(folder2);
        update.setParentId(folder1.getId());

        folderService.update(folder2.getId(), update);
        refreshIndex(1000);

        assertEquals(2, searchService.count(new AssetSearch().setQuery("bilbo")));
        assertEquals(2, searchService.count(new AssetSearch().setQuery("baggins")));
    }

    @Test
    public void testCreateFolderInheritedPermissions() {
        Folder library = folderService.get("/Library");
        Folder orig = folderService.create(new FolderSpec("orig", library));

        assertTrue(orig.getAcl().hasAccess(permissionService.getPermission(Groups.LIBRARIAN), Access.Write));
        assertTrue(orig.getAcl().hasAccess(permissionService.getPermission(Groups.EVERYONE), Access.Read));
        assertEquals(2, orig.getAcl().size());
    }

    @Test
    public void testMoveFolderWithPermmissionChange() {

        Folder library = folderService.get("/Library");
        Folder admin = folderService.get("/Users/admin");
        Folder moving = folderService.create(new FolderSpec("folder_to_move", admin));

        assertTrue(moving.getAcl().hasAccess(permissionService.getPermission("user::admin"), Access.Read));
        assertTrue(moving.getAcl().hasAccess(permissionService.getPermission("user::admin"), Access.Write));
        assertEquals(1, moving.getAcl().size());

        FolderUpdate up = new FolderUpdate(moving);
        up.setParentId(library.getId());

        // Move the folder into the library
        assertTrue(folderService.update(moving.getId(), up));
        Acl acl = folderDao.getAcl(moving.getId());

        assertTrue(acl.hasAccess(permissionService.getPermission(Groups.LIBRARIAN), Access.Write));
        assertTrue(acl.hasAccess(permissionService.getPermission(Groups.EVERYONE), Access.Read));
        assertEquals(2, acl.size());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testUpdateHierarchyFailure() {
        Folder folder1 = folderService.create(new FolderSpec("test3"));
        Folder folder1a = folderService.create(new FolderSpec("test3a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test3b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test3c", folder1b));

        FolderUpdate up = new FolderUpdate(folder1);
        up.setParentId(folder1c.getId());

        folderService.update(folder1.getId(), up);
    }

    @Test(expected=ArchivistWriteException.class)
    public void testUpdateHierarchyFailureSelfAsParent() {
        Folder folder1 = folderService.create(new FolderSpec("test2"));
        Folder folder1a = folderService.create(new FolderSpec("test2a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test2b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test2c", folder1b));
        FolderUpdate up = new FolderUpdate(folder1);
        up.setParentId(folder1.getId());
        folderService.update(folder1.getId(), up);
    }

    @Test
    public void testUpdateRecursive() {
        Folder folder = folderService.create(new FolderSpec("orig"));
        assertTrue(folder.getRecursive());
        FolderUpdate updated = new FolderUpdate(folder);
        updated.setRecursive(false);
        boolean ok = folderService.update(folder.getId(),updated);
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertFalse(revised.getRecursive());
    }

    @Test
    public void testDelete() {
        FolderSpec builder = new FolderSpec("shizzle");
        Folder start = folderService.create(builder);
        Folder root = start;
        for (int i=0; i<10; i++) {
            builder = new FolderSpec("shizzle"+i, start);
            start = folderService.create(builder);
        }
        assertTrue(folderService.delete(root));
    }

    @Test
    public void testDeleteWithDyhi() {
        Folder folder = folderService.create(new FolderSpec("foo"), false);
        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(folder.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day)));
        dyhiService.create(spec);
        assertTrue(folderService.delete(folder));
    }

    @Test
    public void setDyHierarchyTest() {
        Folder folder = folderService.create(new FolderSpec("root"));
        folderService.setDyHierarchyRoot(folder, "source.file");

        folder = folderService.get(folder.getId());
        assertTrue(folder.getSearch().getFilter().getExists().contains("source.file"));
        assertTrue(folder.getDyhiRoot());
    }

    @Test
    public void removeDyHierarchyTest() {
        Folder folder = folderService.create(new FolderSpec("root"));
        folderService.setDyHierarchyRoot(folder, "source.file");

        folder = folderService.get(folder.getId());
        assertTrue(folder.getSearch().getFilter().getExists().contains("source.file"));

        folderService.removeDyHierarchyRoot(folder);
        folder = folderService.get(folder.getId());
        assertNull(folder.getSearch());
        assertFalse(folder.getDyhiRoot());
    }

    @Test
    public void testTrashFolder() {
        int count = folderService.count();

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1));
        assertEquals(count+2, folderService.count());

        TrashedFolderOp result = folderService.trash(folder1);

        // Deleted 2 folders
        assertEquals(2, result.getCount());
        // count back to normal
        assertEquals(count, folderService.count());
    }

    @Test
    public void getTrashedFolder() {
        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        TrashedFolderOp result = folderService.trash(folder1);
        TrashedFolder tf = folderService.getTrashedFolder(result.getTrashFolderId());
    }

    @Test
    public void restoreFolder() {

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1));
        Folder folder3 = folderService.create(new FolderSpec("folder3", folder2));
        TrashedFolderOp result = folderService.trash(folder1);

        // Deleted 2 folders
        assertEquals(3, result.getCount());

        assertEquals(3, folderService.restore(
                folderService.getTrashedFolder(result.getTrashFolderId())).getCount());
    }

    @Test
    public void emptyTrash() {

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1));
        Folder folder3 = folderService.create(new FolderSpec("folder3", folder2));
        TrashedFolderOp result = folderService.trash(folder1);

        // Deleted 2 folders
        assertEquals(3, result.getCount());
        assertEquals(3, folderService.emptyTrash().size());

        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash", Integer.class));
    }

    @Test
    public void isDescendantOf() {

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1));
        Folder folder3 = folderService.create(new FolderSpec("folder3", folder2));

        assertTrue(folderService.isDescendantOf(folder3, folder1));
        assertFalse(folderService.isDescendantOf(folder1, folder3));
        assertTrue(folderService.isDescendantOf(folder3, folderService.get(getRootFolderId())));

    }

    @Test
    public void getAnscestors() {

        Folder folder1 = folderService.create(new FolderSpec("f1"));
        Folder folder2 = folderService.create(new FolderSpec("f2", folder1));
        Folder folder3 = folderService.create(new FolderSpec("f3", folder2));

        List<Folder> folders = folderService.getAllAncestors(folder3, true, false);
    }

    @Test
    public void getFolderByPathWithSpaces() {
        Folder folder1 = folderService.create(new FolderSpec("  f1  "));
        Folder folder2 = folderService.get("/  f1  ");
        assertEquals(folder1, folder2);
    }

    @Test
    public void testCreateUserFolder() {
        PermissionSpec spec = new PermissionSpec("group::wizards");
        Permission perm =  permissionService.createPermission(spec);
        Folder f = folderService.createUserFolder("gandalf", perm);
        assertTrue(f.getAcl().hasAccess(permissionService.getPermission(Groups.EVERYONE), Access.Read));
    }
}
