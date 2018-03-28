package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.FieldDao;
import com.zorroa.archivist.sdk.security.Groups;
import com.zorroa.sdk.domain.AssetIndexSpec;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.LocationSchema;
import com.zorroa.sdk.schema.SourceSchema;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.search.Scroll;
import com.zorroa.sdk.search.SimilarityFilter;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public void testSearchPermissionsMiss() throws IOException {

        authenticate("user");
        Permission perm = permissionService.createPermission(new PermissionSpec("group", "test"));
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToPermissions(perm.getAuthority(), 1);
        Document doc = assetService.index(source);

        refreshIndex();

        AssetSearch search = new AssetSearch().setQuery("beer");
        assertEquals(0, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSearchPermissionsHit() throws IOException {
        authenticate("user");
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", "captain");
        source.addToPermissions(Groups.EVERYONE, 1);
        assetService.index(source);
        refreshIndex();

        AssetSearch search = new AssetSearch().setQuery("beer");
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", source.getAttr("source.filename", String.class));
        Document asset1 = assetService.index(source);
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
        Document asset1 = assetService.index(source);
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
        Document asset1 = assetService.index(source);
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

        builder = new FolderSpec("Age Of Ultron", folder1).setRecursive(false);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.addToKeywords("media", source1.getAttr("source", SourceSchema.class).getFilename());

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.addToKeywords("media", source2.getAttr("source", SourceSchema.class).getFilename());

        Document asset1 = assetService.index(source1);
        Document asset2 = assetService.index(source2);
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

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", "captain");

        Document a = assetService.index(source);
        refreshIndex();

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testTermSearch() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToKeywords("media","captain", "america");

        assetService.index(source);
        logger.info(Json.prettyString(source));
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
        source1.addToKeywords("media", "captain");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.addToKeywords("media", source2.getAttr("source", SourceSchema.class).getFilename());

        assetService.index(source1);
        assetService.index(source2);
        refreshIndex();

        assetService.appendLink("folder", String.valueOf(folder2.getId()), ImmutableList.of(source2.getId()));

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
        source.addToKeywords("media", source.getAttr("source", SourceSchema.class).getFilename());

        assetService.index(source);
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

        assetService.index(source);
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
       source.addToKeywords("media", "zipzoom");
        assetService.index(source);
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
       source.addToKeywords("media","zipzoom");
        assetService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
    }

    @Test
    public void testSearchResponseFields() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToKeywords("media", "zoolander");
        source.addToLinks("folder", 123);
        source.addToLinks("folder", 456);
        assetService.index(source);
        refreshIndex();

        SearchResponse response = searchService.search(new AssetSearch("zoolander"));
        assertEquals(1, response.getHits().getTotalHits());
        Map<String, Object> doc = response.getHits().getAt(0).getSource();
        Document _doc = new Document(doc);

        List<String> folders = _doc.getAttr("zorroa.links.folder", List.class);
        assertEquals(2, folders.size());

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"keywords*"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSource();
        assertNull(doc.get("zorroa.links"));

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"zorroa.links.folder"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSource();
        _doc = new Document(doc);
        folders = folders = _doc.getAttr("zorroa.links.folder", List.class);
        assertEquals(2, folders.size());

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"zorroa.links*"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSource();
        _doc = new Document(doc);
        folders = folders = _doc.getAttr("zorroa.links.folder", List.class);
        assertEquals(2, folders.size());
    }

    @Test
    public void testQueryWildcard() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("media.keywords", ImmutableList.of("source", "zoolander"));
        assetService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoo*")).getHits().getTotalHits());
    }

    @Test
    public void testQueryExactWithQuotes() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("test.keywords", "ironMan17313.jpg");
        assetService.index(source);

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("test.keywords", "ironMan17314.jpg");
        assetService.index(source2);

        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("\"ironMan17313.jpg\"")).getHits().getTotalHits());
    }

    @Test
    public void testQueryMultipleExactWithAnd() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("test.keywords", Lists.newArrayList("RA","pencil","O'Connor"));
        assetService.index(source);

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("test.keywords", Lists.newArrayList("RA","Cock O'the Walk"));
        assetService.index(source2);

        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("\"Cock O'the Walk\" AND \"RA\"")).getHits().getTotalHits());
    }

    @Test
    public void testQueryExactTermWithSpaces() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("test.keywords", Lists.newArrayList("RA", "pencil", "O'Connor"));
        assetService.index(source);

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("test.keywords", Lists.newArrayList("RA", "Cock O'the Walk"));
        assetService.index(source2);

        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("\"Cock O'the Walk\"")).getHits().getTotalHits());

    }

    @Test
    public void testQueryMinusTerm() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", "zoolander", "beer");
        assetService.index(source);
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("zoo* -beer")).getHits().getTotalHits());
    }

    @Test
    public void testQueryPlusTerm() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", "zoolander", "beer");
        assetService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolander OR cat")).getHits().getTotalHits());
    }

    @Test
    public void testQueryExactTerm() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToKeywords("media", "zoolander");
        assetService.index(source);
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("zoolandar")).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zoolander")).getHits().getTotalHits());
    }

    @Test
    public void testQueryFuzzyTerm() throws IOException {
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", "zoolander");
        assetService.index(source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolandar~")).getHits().getTotalHits());
    }

    @Test
    public void testBoostFields() {
        settingsService.set("archivist.search.keywords.boost", "foo:1,bar:2");
        fieldService.invalidateFields();

        Map<String, Set<String>> fields = fieldService.getFields("asset");
        logger.info(Json.prettyString(fields));
        assertTrue(fields.get("keywords-boost").contains("foo:1"));
        assertTrue(fields.get("keywords-boost").contains("bar:2"));
    }

    @Test
    public void getFields() {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"));
        source.setAttr("foo.shash", "AAFFGG");

        assetService.index(source);
        refreshIndex();

        Map<String, Set<String>> fields = fieldService.getFields("asset");
        assertTrue(fields.get("date").size() > 0);
        assertTrue(fields.get("string").size() > 0);
        assertTrue(fields.get("point").size() > 0);
        assertTrue(fields.get("similarity").size() > 0);
        assertTrue(fields.get("keywords").contains("foo.keywords"));
    }

    @Test
    public void getFieldsWithHidden() {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"));
        source.setAttr("foo.shash", "AAFFGG");

        assetService.index(source);
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

        assetService.index(source);
        refreshIndex();
        fieldService.updateField(new HideField("foo.", true));

        Map<String, Set<String>> fields = fieldService.getFields("asset");
        assertFalse(fields.get("keywords-boost").contains("foo.keywords"));
        assertFalse(fields.get("string").contains("foo.keywords"));
    }

    @Test
    public void testScrollSearch() throws IOException {
        assetService.index(new Source(getTestImagePath().resolve("beer_kettle_01.jpg")));
        assetService.index(new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")));
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
        assetService.index(new Source(getTestImagePath().resolve("beer_kettle_01.jpg")));
        assetService.index(new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")));
        refreshIndex();

        PagedList<Document> page = searchService.search(Pager.first(1),
                new AssetSearch().addToAggs("date",
                        ImmutableMap.of("max",
                                ImmutableMap.of("field", "source.fileSize"))));
        assertEquals(1, page.getAggregations().size());
    }

    @Test
    public void testMustSearch() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");

        assetService.index(source1);
        assetService.index(source2);
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

        assetService.index(source1);
        assetService.index(source2);
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

        assetService.index(source1);
        assetService.index(source2);
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

        assetService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
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

        assetService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
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

        assetService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
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

    @Test
    public void testHammingDistanceFilterWithQuery() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.shash", "afafafaf");
        source1.addToKeywords("foo", "bar");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.shash", "adadadad");
        source1.addToKeywords("foo", "bing");

        assetService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search = new AssetSearch("bar");
        search.setFilter(new AssetFilter().addToSimilarity("test.hash1.shash",
                new SimilarityFilter("afafafaf", 8)));

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        float score = searchService.search(search).getHits().hits()[0].getScore();
        assertTrue(score >= .5);
    }

    @Test
    public void testHammingDistanceFilterWithAssetId() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.shash", "afafafaf");
        source1.addToKeywords("foo", "bar");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.shash", "adadadad");
        source1.addToKeywords("foo", "bing");

        assetService.index(new AssetIndexSpec(ImmutableList.of(source2, source1)));
        refreshIndex();

        AssetSearch search = new AssetSearch("bar");
        search.setFilter(new AssetFilter().addToSimilarity("test.hash1.shash",
                new SimilarityFilter(source1.getId(), 8)));

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        float score = searchService.search(search).getHits().hits()[0].getScore();
        assertTrue(score >= .5);
    }

    @Test
    public void testAnalyzeFilenameAsQueryString() throws IOException {
        assertEquals(ImmutableList.of("ironman17314.jpg", "iron", "man", "17314", "jpg"),
                searchService.analyzeQuery(new AssetSearch("ironMan17314.jpg")));
    }


    @Test
    public void testAnalyze() {
        List<String> terms = searchService.analyzeQuery(new AssetSearch("cats dogs"));
        assertTrue(terms != null);
    }

    @Test
    public void testQueryIsAnalyzed() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addToKeywords("media", "zoolander");
        assetService.index(source);
        refreshIndex();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        searchService.search(Pager.first(), new AssetSearch("dog cat"), stream);

        Map<String, Object> result = Json.Mapper.readValue(new ByteArrayInputStream(stream.toByteArray()),
                Json.GENERIC_MAP);
        assertTrue(result.get("queryTerms") != null);
    }

    @Test
    public void testSuggest() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
       source.addToKeywords("media", "zoolander");
        assetService.index(source);
        refreshIndex();
        assertEquals(ImmutableList.of("zoolander"), searchService.getSuggestTerms("zoo"));
    }
}
