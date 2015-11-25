package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.AssetService;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 10/30/15.
 */
public class SearchServiceTests extends ArchivistApplicationTests {


    @Autowired
    AssetDao assetDao;

    @Autowired
    SearchService searchService;

    @Autowired
    UserService userService;

    @Autowired
    FolderService folderService;

    @Autowired
    AssetService assetService;

    @Test
    public void testSearchPermissionsMiss() throws IOException {

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        Permission perm = userService.createPermission(new PermissionBuilder().setName("test").setDescription("test"));

        AssetBuilder builder = new AssetBuilder(filepath);
        builder.setSearchPermissions(perm);
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(0, searchService.search(
                new AssetSearchBuilder().setSearch(search)).getHits().getTotalHits());

    }

    @Test
    public void testSearchPermissionsHit() throws IOException {

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder builder = new AssetBuilder(filepath);
        builder.setSearchPermissions(userService.getPermissions().get(0));
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(1, searchService.search(
                new AssetSearchBuilder().setSearch(search)).getHits().getTotalHits());

    }

    @Test
    public void testFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(assetBuilder);
        refreshIndex(100);

        assetService.addToFolder(asset1, folder1);
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(
                new AssetSearchBuilder().setSearch(search)).getHits().getTotalHits());
    }

    @Test
    public void testRecursiveFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderBuilder("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderBuilder("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(assetBuilder);
        refreshIndex(100);

        assetService.addToFolder(asset1, folder3);
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(
                new AssetSearchBuilder().setSearch(search)).getHits().getTotalHits());
    }
}
