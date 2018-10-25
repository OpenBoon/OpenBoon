package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FieldDao;
import com.zorroa.archivist.schema.LocationSchema;
import com.zorroa.archivist.schema.SourceSchema;
import com.zorroa.archivist.search.*;
import com.zorroa.common.util.Json;
import com.zorroa.security.Groups;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zorroa.archivist.security.UtilsKt.getPermissionsFilter;
import static org.junit.Assert.*;

/**
 * Created by chambers on 10/30/15.
 */
public class SearchServiceTests extends AbstractTest {

    @Autowired
    FieldDao fieldDao;

    @Before
    public void init() {
        cleanElastic();
        fieldService.invalidateFields();
    }

    public void testScanAndScrollClamp() throws IOException {

        addTestAssets("set04");
        refreshIndex();

        int count = 0;
        AssetSearch search = new AssetSearch();
        for (Document doc: searchService.scanAndScroll(search, 2, true)) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testScanAndScrollError() throws IOException {

        addTestAssets("set04");
        refreshIndex();

        int count = 0;
        AssetSearch search = new AssetSearch();
        searchService.scanAndScroll(search, 2, false);
    }

    @Test
    public void testSearchExportPermissionsMiss() throws IOException {

        authenticate("user");
        Permission perm = permissionService.createPermission(new PermissionSpec("group", "test"));
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToPermissions(perm.getAuthority(), 1);
        Document doc = indexService.index(source);

        refreshIndex();

        AssetSearch search = new AssetSearch().setQuery("beer").setAccess(Access.Export);
        assertEquals(0, searchService.search(search).getHits().getTotalHits());
    }

    /**
     * A user with zorroa::export can export anything.
     *
     * @throws IOException
     */
    @Test
    public void testSearchExportPermissionOverrideHit() throws IOException {
        User user = userService.get("user");
        userService.addPermissions(user, ImmutableList.of(
                permissionService.getPermission("zorroa::export")));
        authenticate("user");
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        indexService.index(source);
        refreshIndex();
        AssetSearch search = new AssetSearch().setQuery("source.filename:beer").setAccess(Access.Export);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
        assertNull(getPermissionsFilter(search.getAccess()));
    }

    @Test
    public void testSearchPermissionsExportHit() throws IOException {
        Permission perm = permissionService.createPermission(new PermissionSpec("group", "test"));
        User user = userService.get("user");
        userService.addPermissions(user, ImmutableList.of(perm));
        authenticate("user");

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToPermissions(perm.getAuthority(), Access.Export.value);
        indexService.index(source);
        refreshIndex();

        AssetSearch search = new AssetSearch()
                .setAccess(Access.Export)
                .setQuery("source.filename:beer");
        assertNotNull(getPermissionsFilter(search.getAccess()));
        assertEquals(1, searchService.search(search).getHits().getTotalHits());

    }

    @Test
    public void testSearchPermissionsReadMiss() throws IOException {

        authenticate("user");
        Permission perm = permissionService.createPermission(new PermissionSpec("group", "test"));
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToPermissions(perm.getAuthority(), 1);
        Document doc = indexService.index(source);

        refreshIndex();

        AssetSearch search = new AssetSearch().setQuery("beer");
        assertEquals(0, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSearchPermissionsReadHit() throws IOException {
        authenticate("user");
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("captain"));
        source.addToPermissions(Groups.EVERYONE, 1);
        indexService.index(source);
        refreshIndex();

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", source.getAttr("source.filename", String.class));
        Document asset1 = indexService.index(source);
        refreshIndex(100);

        folderService.addAssets(folder1, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testFolderCount() throws IOException {

        FolderSpec builder = new FolderSpec("Beer");
        Folder folder1 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", source.getAttr("source.filename", String.class));
        Document asset1 = indexService.index(source);
        refreshIndex(100);

        folderService.addAssets(folder1, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        assertEquals(1, searchService.count(folder1));
    }


    @Test
    public void testRecursiveFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderSpec("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Document asset1 = indexService.index(source);
        refreshIndex(100);

        folderService.addAssets(folder3, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testNonRecursiveFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderSpec("Age Of Ultron", folder1);
        builder.setRecursive(false);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.addToKeywords("media", source1.getAttr("source", SourceSchema.class).getFilename());

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.addToKeywords("media", source2.getAttr("source", SourceSchema.class).getFilename());

        Document asset1 = indexService.index(source1);
        Document asset2 = indexService.index(source2);
        refreshIndex();

        folderService.addAssets(folder2, Lists.newArrayList(asset2.getId()));
        folderService.addAssets(folder3, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);

        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSmartFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderSpec("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        builder.setSearch(new AssetSearch("captain america"));
        Folder folder3 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("captain"));

        Document a = indexService.index(source);
        refreshIndex();

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testTermSearch() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("captain", "america"));

        indexService.index(source);
        refreshIndex();

        int count = 0;
        for (Document a: searchService.search(Pager.first(), new AssetSearch().setFilter(
                new AssetFilter().addToTerms("media.keywords", "captain")))) {
            count++;
        }
        assertTrue(count > 0);
    }

    @Test
    public void testSmartFolderAndStaticFolderMixture() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderSpec("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        builder.setSearch(new AssetSearch("captain america"));
        Folder folder3 = folderService.create(builder);

        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("media.keywords", ImmutableList.of("captain"));

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("media.keywords", source2.getAttr("source", SourceSchema.class).getFilename());

        indexService.index(source1);
        indexService.index(source2);
        refreshIndex();

        indexService.appendLink("folder", String.valueOf(folder2.getId()), ImmutableList.of(source2.getId()));

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(2, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testLotsOfSmartFolders() throws IOException {

        FolderSpec builder = new FolderSpec("people");
        Folder folder1 = folderService.create(builder);

        for (int i=0; i<100; i++) {
            builder = new FolderSpec("person" + i, folder1);
            builder.setSearch(new AssetSearch("beer"));
            folderService.create(builder);
        }

        refreshIndex();

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", source.getAttr("source", SourceSchema.class).getFilename());

        indexService.index(source);
        refreshIndex();

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testQueryWithCustomFields() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("foo.bar1", "captain kirk");
        source.setAttr("foo.bar2", "bilbo baggins");
        source.setAttr("foo.bar3", "pirate pete");

        indexService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("captain").setQueryFields(ImmutableMap.of("foo.bar1", 1.0f))).getHits().getTotalHits());
        assertEquals(0, searchService.search(
                new AssetSearch("captain").setQueryFields(ImmutableMap.of("foo.bar2", 1.0f))).getHits().getTotalHits());

        assertEquals(1, searchService.search(
                new AssetSearch("captain baggins").setQueryFields(
                        ImmutableMap.of("foo.bar1", 1.0f, "foo.bar2", 2.0f))).getHits().getTotalHits());


    }

    @Test
    public void testHighConfidenceSearch() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("zipzoom"));
        indexService.index(source);
        refreshIndex();

        /*
         * High confidence words are found at every level.
         */
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
    }

    @Test
    public void testNoConfidenceSearch() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords",ImmutableList.of("zipzoom"));
        indexService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
    }

    @Test
    public void testSearchResponseFields() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("zooland"));
        source.addToLinks("folder", "abc123");
        source.addToLinks("folder", "abc456");
        indexService.index(source);
        refreshIndex();

        SearchResponse response = searchService.search(new AssetSearch("zoolander"));
        assertEquals(1, response.getHits().getTotalHits());
        Map<String, Object> doc = response.getHits().getAt(0).getSourceAsMap();
        Document _doc = new Document(doc);

        List<String> folders = _doc.getAttr("system.links.folder", List.class);
        assertEquals(2, folders.size());

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"keywords*"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSourceAsMap();
        assertNull(doc.get("system.links"));

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"system.links.folder"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSourceAsMap();
        _doc = new Document(doc);
        folders = folders = _doc.getAttr("system.links.folder", List.class);
        assertEquals(2, folders.size());

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"system.links*"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSourceAsMap();
        _doc = new Document(doc);
        folders = folders = _doc.getAttr("system.links.folder", List.class);
        assertEquals(2, folders.size());
    }

    @Test
    public void testQueryWildcard() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("source", "zoolander"));
        indexService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoo*")).getHits().getTotalHits());
    }

    @Test
    public void testQueryExactWithQuotes() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("test.keywords", "ironMan17313.jpg");
        indexService.index(source);

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("test.keywords", "ironMan17314.jpg");
        indexService.index(source2);

        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("\"ironMan17313.jpg\"")).getHits().getTotalHits());
    }

    @Test
    public void testQueryMultipleExactWithAnd() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("test.keywords", Lists.newArrayList("RA","pencil","O'Connor"));
        indexService.index(source);

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("test.keywords", Lists.newArrayList("RA","Cock O'the Walk"));
        indexService.index(source2);

        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("\"Cock O'the Walk\" AND \"RA\"")).getHits().getTotalHits());
    }

    @Test
    public void testQueryExactTermWithSpaces() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("test.keywords", Lists.newArrayList("RA", "pencil", "O'Connor"));
        indexService.index(source);

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("test.keywords", Lists.newArrayList("RA", "Cock O'the Walk"));
        indexService.index(source2);

        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("\"Cock O'the Walk\"")).getHits().getTotalHits());

    }

    @Test
    public void testQueryMinusTerm() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", "zoolander", "beer");
        indexService.index(source);
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("zoo* -beer")).getHits().getTotalHits());
    }

    @Test
    public void testQueryPlusTerm() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("zoolander", "beer"));
        indexService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolander OR cat")).getHits().getTotalHits());
    }

    @Test
    public void testQueryExactTerm() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("zooland"));
        indexService.index(source);
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("zoolandar")).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zoolander")).getHits().getTotalHits());
    }

    @Test
    public void testQueryFuzzyTerm() throws IOException {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("zooland"));
        indexService.index(source);
        getLogger().info("{}", Json.INSTANCE.prettyString(source));
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolind~")).getHits().getTotalHits());
    }

    @Test
    public void testBoostFields() {
        settingsService.set("archivist.search.keywords.boost", "foo:1,bar:2");
        fieldService.invalidateFields();

        Map<String, Set<String>> fields = fieldService.getFields("asset");
        assertTrue(fields.get("keywords-boost").contains("foo:1"));
        assertTrue(fields.get("keywords-boost").contains("bar:2"));
    }

    @Test
    public void getFields() {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"));
        source.setAttr("foo.shash", "AAFFGG");
        source.setAttr("media.clip.parent", "abc123");

        indexService.index(source);
        refreshIndex();

        Map<String, Set<String>> fields = fieldService.getFields("asset");
        assertTrue(fields.get("date").size() > 0);
        assertTrue(fields.get("string").size() > 0);
        assertTrue(fields.get("point").size() > 0);
        assertTrue(fields.get("similarity").size() > 0);
        assertTrue(fields.get("id").size() > 0);
        assertTrue(fields.get("keywords").contains("foo.keywords"));
    }

    @Test
    public void getFieldsWithHidden() {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"));
        source.setAttr("foo.shash", "AAFFGG");

        indexService.index(source);
        refreshIndex();
        fieldService.updateField(new HideField("foo.keywords", true));

        Map<String, Set<String>> fields = fieldService.getFields("asset");
        assertFalse(fields.get("keywords-boost").contains("foo.keywords"));
        assertFalse(fields.get("string").contains("foo.keywords"));
    }

    @Test
    public void getFieldsWithHiddenNameSpace() {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"));
        source.setAttr("foo.shash", "AAFFGG");

        indexService.index(source);
        refreshIndex();
        fieldService.updateField(new HideField("foo.", true));

        Map<String, Set<String>> fields = fieldService.getFields("asset");
        assertFalse(fields.get("keywords-boost").contains("foo.keywords"));
        assertFalse(fields.get("string").contains("foo.keywords"));
    }

    @Test
    public void testScrollSearch() throws IOException {
        indexService.index(new Source(getTestImagePath().resolve("beer_kettle_01.jpg")));
        indexService.index(new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")));
        refreshIndex();

        PagedList<Document> result1 =
                searchService.search(Pager.first(1),
                        new AssetSearch().setScroll(new Scroll().setTimeout("1m")));
        assertNotNull(result1.getScroll());
        assertEquals(1, result1.size());

        PagedList<Document> result2 =
                searchService.search(Pager.first(1), new AssetSearch().setScroll(result1.getScroll()
                        .setTimeout("1m")));
        assertNotNull(result2.getScroll());
        assertEquals(1, result2.size());
    }

    @Test
    public void testAggregationSearch() throws IOException {
        indexService.index(new Source(getTestImagePath().resolve("beer_kettle_01.jpg")));
        indexService.index(new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")));
        refreshIndex();

        PagedList<Document> page = searchService.search(Pager.first(1),
                new AssetSearch().addToAggs("foo",
                        ImmutableMap.of("max",
                                ImmutableMap.of("field", "source.fileSize"))));
        assertEquals(1, page.getAggregations().size());
    }

    @Test
    public void testAggregationSearchEmptyFilter() throws IOException {
        indexService.index(new Source(getTestImagePath().resolve("beer_kettle_01.jpg")));
        indexService.index(new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")));
        refreshIndex();

        Map<String,Object> req = Maps.newHashMap();
        req.put("filter",  ImmutableMap.of());
        req.put("aggs", ImmutableMap.of("foo", ImmutableMap.of("terms",
                ImmutableMap.of("field", "source.fileSize"))));

        PagedList<Document> page = searchService.search(Pager.first(1),
                new AssetSearch().addToAggs("facet", req));
        assertEquals(1, page.getAggregations().size());
    }

    @Test
    public void testMustSearch() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");

        indexService.index(source1);
        indexService.index(source2);
        refreshIndex();

        AssetFilter filter = new AssetFilter().setMust(ImmutableList.of(new AssetFilter().addToTerms("superhero", "captain")));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testMustNotSearch() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");

        indexService.index(source1);
        indexService.index(source2);
        refreshIndex();

        AssetFilter filter = new AssetFilter().setMustNot(ImmutableList.of(new AssetFilter().addToTerms("superhero", "captain")));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testShouldSearch() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");

        indexService.index(source1);
        indexService.index(source2);
        refreshIndex();

        AssetFilter filter = new AssetFilter().setShould(ImmutableList.of(new AssetFilter().addToTerms("superhero", "captain")));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testHammingDistanceFilterWithEmptyValue() throws IOException, InterruptedException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.jimbo", "");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");

        indexService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search;

        search = new AssetSearch(
                new AssetFilter().addToSimilarity("test.hash1.jimbo",
                        new SimilarityFilter("AFAFAFAF", 100)));
        assertEquals(0, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testHammingDistanceFilterWithRaw() throws IOException, InterruptedException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.jimbo", "AFAFAFAF");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.jimbo", "ADADADAD");

        indexService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search;

        search = new AssetSearch(
                new AssetFilter().addToSimilarity("test.hash1.jimbo.raw",
                        new SimilarityFilter("AFAFAFAF", 100)));
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testHammingDistanceFilter() throws IOException, InterruptedException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.shash", "AFAFAFAF");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.shash", "ADADADAD");

        indexService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search;

        search = new AssetSearch(
                new AssetFilter().addToSimilarity( "test.hash1.shash",
                        new SimilarityFilter("AFAFAFAF", 100)));
        assertEquals(1, searchService.search(search).getHits().getTotalHits());

        search = new AssetSearch(
                new AssetFilter().addToSimilarity( "test.hash1.shash",
                        new SimilarityFilter("AFAFAFAF", 50)));
        assertEquals(2, searchService.search(search).getHits().getTotalHits());

        search = new AssetSearch(
                new AssetFilter().addToSimilarity( "test.hash1.shash",
                        new SimilarityFilter("APAPAPAP", 20)));

        assertEquals(2, searchService.search(search).getHits().getTotalHits());

    }

    /**
     * The array handling code needs to be updated in es-similarity.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Ignore
    @Test
    public void testHammingDistanceFilterArray() throws IOException, InterruptedException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.shash", ImmutableList.of("AFAFAFAF", "AFAFAFA1"));

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.shash",  ImmutableList.of("ADADADAD"));

        indexService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search;

        search = new AssetSearch(
                new AssetFilter().addToSimilarity( "test.hash1.shash",
                        new SimilarityFilter("AFAFAFAF",    100)));
        SearchHits hits = searchService.search(search).getHits();
        assertEquals(1, hits.getTotalHits());
        Document doc = new Document(hits.getAt(0).getSourceAsMap());
        assertEquals(ImmutableList.of("AFAFAFAF", "AFAFAFA1"), doc.getAttr("test.hash1.shash"));

    }

    @Test
    public void testHammingDistanceFilterWithQuery() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("media.keywords", Lists.newArrayList("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.shash", "afafafaf");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.shash", "adadadad");

        indexService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search = new AssetSearch("beer");
        search.setFilter(new AssetFilter().addToSimilarity("test.hash1.shash",
                new SimilarityFilter("afafafaf", 1)));

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        float score = searchService.search(search).getHits().getAt(0).getScore();
        assertTrue(score >= .5);
    }

    @Test
    public void testHammingDistanceFilterWithAssetId() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("media.keywords", Lists.newArrayList("beer"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.shash", "afafafaf");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.shash", "adadadad");

        indexService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search = new AssetSearch("beer");
        search.setFilter(new AssetFilter().addToSimilarity("test.hash1.shash",
                new SimilarityFilter(source1.getId(), 8)));

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        float score = searchService.search(search).getHits().getAt(0).getScore();
        assertTrue(score >= .5);
    }

    @Test
    public void testSuggest() {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("zoolander"));

        indexService.index(source);
        refreshIndex();
        assertEquals(ImmutableList.of("zoolander"), searchService.getSuggestTerms("zoo"));
    }

    @Test
    public void testEmptySearch() {
        addTestAssets("set01");
        refreshIndex();
        assertEquals(5, searchService.search(Pager.first(), new AssetSearch()).size());
    }

    @Test
    public void testFromSize() {
        addTestAssets("set01");
        refreshIndex();
        assertEquals(2,
                searchService.search(new Pager(2, 2), new AssetSearch()).size());
    }

    @Test
    public void testFilterExists() {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        indexService.index(source);
        refreshIndex();

        AssetSearch asb = new AssetSearch(new AssetFilter().addToExists("source.path"));
        PagedList result = searchService.search(Pager.first(), asb);
        assertEquals(1, result.size());

        asb = new AssetSearch(new AssetFilter().addToExists("source.dsdsdsds"));
       result = searchService.search(Pager.first(), asb);
        assertEquals(0, result.size());

    }

    @Test
    public void testFilterMissing() {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        indexService.index(source);
        refreshIndex();

        AssetSearch asb = new AssetSearch(new AssetFilter().addToMissing("source.path"));
        PagedList result = searchService.search(Pager.first(), asb);
        assertEquals(0, result.size());

        asb = new AssetSearch(new AssetFilter().addToMissing("source.dsdsdsds"));
        result = searchService.search(Pager.first(), asb);
        assertEquals(1, result.size());
    }

    @Test
    public void testFilterRange() {
        addTestAssets("set01");
        AssetSearch asb = new AssetSearch(new AssetFilter()
                .addRange("source.fileSize", new RangeQuery().setGt(100000)));
        PagedList result = searchService.search(Pager.first(), asb);
        assertEquals(2, result.size());
    }

    @Test
    public void testFilterScript()  {
        addTestAssets("set01");

        String text = "doc['source.fileSize'].value == params.size";
        AssetScript script = new AssetScript(text,
                ImmutableMap.of("size", 113333));

        List<AssetScript> scripts = new ArrayList<>();
        scripts.add(script);
        AssetSearch asb = new AssetSearch(new AssetFilter().setScripts(scripts));
        PagedList result = searchService.search(Pager.first(), asb);
        assertEquals(1, result.size());
    }

}
