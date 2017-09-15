package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public void testAddAssetToFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);

        Map<String, List<Object>> results = folderService.addAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());
    }

    @Test
    public void testAddDuplicateAssetsToFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);

        folderService.addAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        folderService.addAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        refreshIndex();

        PagedList<Asset> assets = assetService.getAll(Pager.first());
        for (Asset a: assets) {
            assertEquals(1, ((List) a.getAttr("links.folder")).size());
        }
    }

    @Test
    public void testRemoveAssetFromFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);

        Map<String, List<Object>> results = folderService.addAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));

        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());

        results = folderService.removeAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());
    }

    @Test
    public void testRemoveAssetFromTaxonomyFolder() {

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        taxonomyService.create(new TaxonomySpec(folder));
        folder = folderService.get(folder.getId());

        Map<String, List<Object>> results = folderService.addAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        refreshIndex();

        assertEquals(2, searchService.search(new AssetSearch(
                new AssetFilter().addToTerms("links.folder", folder.getId()))).getHits().getTotalHits());
        assertEquals(2, searchService.search(new AssetSearch("Folder")).getHits().getTotalHits());

        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());

        results = folderService.removeAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
        assertTrue(results.get("failed").isEmpty());
        assertFalse(results.get("success").isEmpty());
        refreshIndex();

        assertEquals(0, searchService.search(new AssetSearch(
                new AssetFilter().addToTerms("links.folder", folder.getId()))).getHits().getTotalHits());
        assertEquals(0, searchService.search(new AssetSearch("Folder")).getHits().getTotalHits());

    }

    @Test
    public void testCountAssetSmartFolder() {
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.update(folder.getId(), folder.setSearch(new AssetSearch("jpg")));
        assertEquals(2, searchService.count(folderService.get(folder.getId())));

        folderService.update(folder.getId(), folder.setSearch(new AssetSearch("wdsdsdsdsds")));
        assertEquals(0, searchService.count(folderService.get(folder.getId())));
    }

    @Test
    public void testSetAcl() {
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        folderService.setAcl(folder, new Acl().addEntry(
                userService.getPermission("group::manager"),
                Access.Read, Access.Write, Access.Export), false);
        folderService.get(folder.getId());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetAclFailure() {
        authenticate("manager");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        /**
         * Since we have all permissions now, this should fail because we
         * are taking away write/export permissions from ourself.
         */
        folderService.setAcl(folder, new Acl().addEntry(
                userService.getPermission("group::manager"),
                Access.Read), false);
        folderService.get(folder.getId());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetAclFailureDoesntHavePermission() {
        authenticate("manager");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        /**
         * Since we have all permissions now, this should fail because we
         * are taking away write/export permissions from ourself.
         */
        folderService.setAcl(folder, new Acl().addEntry(
                userService.getPermission("group::administrator"),
                Access.Read), false);
        folderService.get(folder.getId());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testGetFolderWithoutAcl() {
        authenticate("manager");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        // use the DAO so don't fail the remove access from self check.
        folderDao.setAcl(folder.getId(), new Acl().addEntry(
                userService.getPermission("group::administrator"), Access.Read));
        folderService.invalidate(folder);
        folderService.get(folder.getId());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testCreateWithReadAcl() {
        authenticate("manager");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderDao.setAcl(folder.getId(),
                new Acl().addEntry(userService.getPermission("group::administrator"), Access.Read));
        folderService.get(folder.getId());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testAddAssetsWithWriteAcl() {
        authenticate("manager");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        Acl acl = new Acl().addEntry(userService.getPermission("group::administrator"), Access.Write);
        folderDao.setAcl(folder.getId(), acl);
        folder.setAcl(acl);

        folderService.addAssets(folder, assetService.getAll(
                Pager.first()).stream().map(a->a.getId()).collect(Collectors.toList()));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testDeleteFolderWithWriteAcl() {
        authenticate("manager");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        Acl acl = new Acl().addEntry(userService.getPermission("group::administrator"), Access.Write);
        folder.setAcl(acl);
        folderDao.setAcl(folder.getId(), acl);
        folderService.delete(folder);
    }

    @Test(expected=ArchivistWriteException.class)
    public void testUpdateFolderWithWriteAcl() {
        authenticate("manager");
        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderDao.setAcl(folder.getId(),
                new Acl().addEntry(userService.getPermission("group::administrator"), Access.Write));
        folderService.update(folder.getId(), folder.setName("biblo"));
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
        Folder dad = folderService.create(new FolderSpec("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderSpec("uncle", grandpa.getId()));
        folderService.create(new FolderSpec("child", dad.getId()));
        folderService.create(new FolderSpec("cousin", uncle.getId()));
        List<Folder> descendents = folderService.getAllDescendants(grandpa, false);
        assertEquals(4, descendents.size());
    }

    @Test
    public void testGetAllDescendants() {
        Folder grandpa = folderService.create(new FolderSpec("grandpa"));
        Folder dad = folderService.create(new FolderSpec("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderSpec("uncle", grandpa.getId()));
        folderService.create(new FolderSpec("child", dad.getId()));
        folderService.create(new FolderSpec("cousin", uncle.getId()));
        assertEquals(5, folderService.getAllDescendants(Lists.newArrayList(grandpa), true, true).size());
        assertEquals(4, folderService.getAllDescendants(Lists.newArrayList(grandpa), false, true).size());

        logger.info("{}", folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), true, true));

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
        assertEquals(folder1b.getId(), folder.getParentId().intValue());
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
        boolean ok = folderService.update(folder.getId(), folder.setName("new"));
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
        boolean ok = folderService.update(folder2.getId(), folder2.setParentId(folder1.getId()));
        assertTrue(ok);

        Folder revised = folderService.get(folder2.getId());
        assertEquals(folder1.getId(), revised.getParentId().intValue());

        List<Folder> folders = folderService.getAllDescendants(Lists.newArrayList(folder1), false, false);
        assertTrue(folders.contains(folder2));

        folders = folderService.getAllDescendants(Lists.newArrayList(folder2), false, false);
        assertTrue(folders.isEmpty());
    }

    @Test
    public void testCreateFolderInheritedPermissions() {
        Folder library = folderService.get("/Library");
        Folder orig = folderService.create(new FolderSpec("orig", library));

        assertTrue(orig.getAcl().hasAccess(userService.getPermission("group::manager"), Access.Write));
        assertTrue(orig.getAcl().hasAccess(userService.getPermission("group::everyone"), Access.Read));
        assertEquals(2, orig.getAcl().size());
    }

    @Test
    public void testMoveFolderWithPermmissionChange() {

        Folder library = folderService.get("/Library");

        Folder admin = folderService.get("/Users/admin");
        Folder moving = folderService.create(new FolderSpec("folder_to_move", admin));

        assertTrue(moving.getAcl().hasAccess(userService.getPermission("user::admin"), Access.Read));
        assertTrue(moving.getAcl().hasAccess(userService.getPermission("user::admin"), Access.Write));
        assertEquals(1, moving.getAcl().size());

        // Move the folder into the library
        assertTrue(folderService.update(moving.getId(), moving.setParentId(library.getId())));
        Acl acl = folderDao.getAcl(moving.getId());

        assertTrue(moving.getAcl().hasAccess(userService.getPermission("group::manager"), Access.Write));
        assertTrue(moving.getAcl().hasAccess(userService.getPermission("group::everyone"), Access.Read));
        assertEquals(2, moving.getAcl().size());
    }

    @Test(expected=ArchivistWriteException.class)
    public void testUpdateHierarchyFailure() {
        Folder folder1 = folderService.create(new FolderSpec("test3"));
        Folder folder1a = folderService.create(new FolderSpec("test3a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test3b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test3c", folder1b));
        folderService.update(folder1.getId(), folder1.setParentId(folder1c.getId()));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testUpdateHierarchyFailureSelfAsParent() {
        Folder folder1 = folderService.create(new FolderSpec("test2"));
        Folder folder1a = folderService.create(new FolderSpec("test2a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test2b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test2c", folder1b));
        folderService.update(folder1.getId(), folder1.setParentId(folder1.getId()));
    }

    @Test
    public void testUpdateRecursive() {
        Folder folder = folderService.create(new FolderSpec("orig"));
        assertTrue(folder.isRecursive());
        boolean ok = folderService.update(folder.getId(), folder.setRecursive(false));
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertFalse(revised.isRecursive());
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
        assertTrue(folder.isDyhiRoot());
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
        assertFalse(folder.isDyhiRoot());
    }

    @Test
    public void testTrashFolder() {
        int count = folderService.count();

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1.getId()));
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
        logger.info(Json.serializeToString(tf));
    }

    @Test
    public void restoreFolder() {

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("folder3", folder2.getId()));
        TrashedFolderOp result = folderService.trash(folder1);

        // Deleted 2 folders
        assertEquals(3, result.getCount());

        assertEquals(3, folderService.restore(
                folderService.getTrashedFolder(result.getTrashFolderId())).getCount());
    }

    @Test
    public void emptyTrash() {

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("folder3", folder2.getId()));
        TrashedFolderOp result = folderService.trash(folder1);

        // Deleted 2 folders
        assertEquals(3, result.getCount());
        assertEquals(3, folderService.emptyTrash().size());

        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash", Integer.class));
    }

    @Test
    public void isDescendantOf() {

        Folder folder1 = folderService.create(new FolderSpec("folder1"));
        Folder folder2 = folderService.create(new FolderSpec("folder2", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("folder3", folder2.getId()));

        assertTrue(folderService.isDescendantOf(folder3, folder1));
        assertFalse(folderService.isDescendantOf(folder1, folder3));
        assertTrue(folderService.isDescendantOf(folder3, folderService.get(0)));

    }

    @Test
    public void getAnscestors() {

        Folder folder1 = folderService.create(new FolderSpec("f1"));
        Folder folder2 = folderService.create(new FolderSpec("f2", folder1.getId()));
        Folder folder3 = folderService.create(new FolderSpec("f3", folder2.getId()));

        List<Folder> folders = folderService.getAllAncestors(folder3, true, false);
        logger.info("{}", folders);

    }
}
