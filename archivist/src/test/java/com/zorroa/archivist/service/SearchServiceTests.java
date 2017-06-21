package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.PermissionSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.Color;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.LinkSchema;
import com.zorroa.sdk.schema.LocationSchema;
import com.zorroa.sdk.schema.SourceSchema;
import com.zorroa.sdk.search.*;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by chambers on 10/30/15.
 */
public class SearchServiceTests extends AbstractTest {


    @Test
    public void testSearchPermissionsMiss() throws IOException {

        authenticate("user");
        Permission perm = userService.createPermission(new PermissionSpec("group", "test"));
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));

        SecurityUtils.setReadPermissions(source, Lists.newArrayList(perm));
        assetService.index(source);
        refreshIndex();

        AssetSearch search = new AssetSearch().setQuery("beer");
        assertEquals(0, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSearchPermissionsHit() throws IOException {
        authenticate("admin");

        Permission perm = userService.createPermission(new PermissionSpec("group", "test"));
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", "captain");
        /*
         * Add a permission from the current user to the asset.
         */
        SecurityUtils.setReadPermissions(source, Lists.newArrayList(userService.getPermissions(SecurityUtils.getUser()).get(0)));
        assetService.index(source);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", source.getAttr("source.filename", String.class));
        Asset asset1 = assetService.index(source);
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
        source.addKeywords("source", source.getAttr("source.filename", String.class));
        Asset asset1 = assetService.index(source);
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
        Asset asset1 = assetService.index(source);
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
        source1.addKeywords("source", source1.getAttr("source", SourceSchema.class).getFilename());

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.addKeywords("source", source2.getAttr("source", SourceSchema.class).getFilename());

        Asset asset1 = assetService.index(source1);
        Asset asset2 = assetService.index(source2);
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
        source.addKeywords("source", "captain");

        Asset a = assetService.index(source);
        refreshIndex();

        AssetFilter filter = new AssetFilter().addToLinks("folder", folder1.getId());
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testTermSearch() throws IOException {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", "captain", "america");

        assetService.index(source);
        refreshIndex();

        int count = 0;
        for (Asset a: searchService.search(Pager.first(), new AssetSearch().setFilter(
                new AssetFilter().addToTerms("keywords.source", "captain")))) {
            assertTrue(a.getScore() > 0);
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
        source1.addKeywords("source", "captain");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.addKeywords("source", source2.getAttr("source", SourceSchema.class).getFilename());

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
        source.addKeywords("source", source.getAttr("source", SourceSchema.class).getFilename());

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

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zipzoom");
        assetService.index(Source);
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

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source","zipzoom");
        assetService.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
    }

    @Test
    public void testSearchResponseFields() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander");
        LinkSchema links = Source.getAttr("links");
        links.addLink("folder", 123);
        links.addLink("folder", 456);
        Source.setAttr("links", links);
        assetService.index(Source);
        refreshIndex();

        SearchResponse response = searchService.search(new AssetSearch("zoolander"));
        assertEquals(1, response.getHits().getTotalHits());
        Map<String, Object> doc = response.getHits().getAt(0).getSource();
        ArrayList<Integer> folders = (ArrayList<Integer>)((Map<String, Object>)doc.get("links")).get("folder");
        assertEquals(2, folders.size());

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"keywords*"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSource();
        assertNull(doc.get("links"));

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"links.folder"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSource();
        folders = (ArrayList<Integer>)((Map<String, Object>)doc.get("links")).get("folder");
        assertEquals(2, folders.size());

        response = searchService.search(new AssetSearch("zoolander").setFields(new String[]{"links*"}));
        assertEquals(1, response.getHits().getTotalHits());
        doc = response.getHits().getAt(0).getSource();
        folders = (ArrayList<Integer>)((Map<String, Object>)doc.get("links")).get("folder");
        assertEquals(2, folders.size());
    }

    @Test
    public void testSimpleQuery() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander");
        assetService.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoo*")).getHits().getTotalHits());
    }

    @Test
    public void testQueryWithSingleQuote() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "O'Malley");
        Source.addKeywords("source", "beer");
        assetService.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("O'Malley")).getHits().getTotalHits());
    }

    @Test
    public void testQuotedString() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "O bummer");
        assetService.index(Source);

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.addKeywords("source", "O bummer again");
        assetService.index(source2);

        refreshIndex();

        assertEquals(2, searchService.search(
                new AssetSearch("\"O bummer\"")).getHits().getTotalHits());
    }

    @Test
    public void testMinusQuery() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander", "beer");
        assetService.index(Source);
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("zoo* -beer")).getHits().getTotalHits());
    }

    @Test
    public void testOrQuery() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander", "beer");
        assetService.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolander OR cat")).getHits().getTotalHits());
    }

    @Test
    public void testExactSearch() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander");
        assetService.index(Source);
        refreshIndex();

        assertEquals(0, searchService.search(
                new AssetSearch("zoolandar")).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zoolander")).getHits().getTotalHits());
    }

    @Test
    public void testManualFuzzySearch() throws IOException {
        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander");
        assetService.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolandar~")).getHits().getTotalHits());
    }

    @Test
    public void getFields() {

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"));
        source.setAttr("foo.byte", "AAFFGG");

        assetService.index(source);
        refreshIndex();

        Map<String, Set<String>> fields = searchService.getFields();
        assertTrue(fields.get("date").size() > 0);
        assertTrue(fields.get("string").size() > 0);
        assertTrue(fields.get("integer").size() > 0);
        assertTrue(fields.get("point").size() > 0);
        assertTrue(fields.get("hash").size() > 0);
        assertTrue(fields.get("keywords-auto").contains("foo.keywords"));
    }

    @Test
    public void testColorSearch() {
        Color color = new Color(255, 10, 10).setRatio(50f);

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.setAttr("colors", ImmutableList.of(color));
        assetService.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch().setFilter(new AssetFilter().putToColors("colors",
                        new ColorFilter()
                        .setMinRatio(45)
                        .setMaxRatio(55)
                        .setHueAndRange(0, 5)
                        .setSaturationAndRange(100, 5)
                        .setBrightnessAndRange(50, 5)))).getHits().getTotalHits());
    }

    @Test
    public void testScrollSearch() throws IOException {
        assetService.index(new Source(getTestImagePath().resolve("beer_kettle_01.jpg")));
        assetService.index(new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")));
        refreshIndex();

        PagedList<Asset> result1 =
                searchService.search(Pager.first(1),
                        new AssetSearch().setScroll(new Scroll().setTimeout("1m")));
        assertNotNull(result1.getScroll());
        assertEquals(1, result1.size());

        PagedList<Asset> result2 =
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

        PagedList<Asset> page = searchService.search(Pager.first(1),
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
    public void testHammingDistanceFilter() throws IOException, InterruptedException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.byte", "AFAFAFAF");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.byte", "ADADADAD");

        assetService.index(ImmutableList.of(source2, source1));
        refreshIndex();

        AssetSearch search;

        search = new AssetSearch(
                new AssetFilter().setHamming(
                        new HammingDistanceFilter("AFAFAFAF", "test.hash1.byte", 100)));
        assertEquals(1, searchService.search(search).getHits().getTotalHits());

        search = new AssetSearch(
                new AssetFilter().setHamming(
                        new HammingDistanceFilter("AFAFAFAF", "test.hash1.byte", 50)));
        assertEquals(2, searchService.search(search).getHits().getTotalHits());

        search = new AssetSearch(
                new AssetFilter().setHamming(
                        new HammingDistanceFilter("APAPAPAP", "test.hash1.byte", 20)));

        assertEquals(2, searchService.search(search).getHits().getTotalHits());

    }

    @Test
    public void testHammingDistanceFilterWithQuery() throws IOException {
        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.setAttr("superhero", "captain");
        source1.setAttr("test.hash1.byte", "afafafaf");
        source1.addKeywords("foo", "bar");

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.setAttr("superhero", "loki");
        source2.setAttr("test.hash1.byte", "adadadad");
        source1.addKeywords("foo", "bing");

        assetService.index(ImmutableList.of(source1, source2));
        refreshIndex();

        AssetSearch search = new AssetSearch("bar");
        search.setFilter(new AssetFilter().setHamming(
                new HammingDistanceFilter("afafafaf", "test.hash1.byte", 8)));

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        float score = searchService.search(search).getHits().hits()[0].getScore();
        assertTrue(score >= 100);
    }

    @Test
    public void testAnalyze() {
        List<String> terms = searchService.analyzeQuery(new AssetSearch("cats dogs"));
        assertEquals(ImmutableList.of("cats", "dogs"), terms);
    }

    @Test
    public void testQueryIsAnalyzed() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander");
        assetService.index(Source);
        refreshIndex();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        searchService.search(Pager.first(), new AssetSearch("dog cat"), stream);

        Map<String, Object> result = Json.Mapper.readValue(new ByteArrayInputStream(stream.toByteArray()),
                Json.GENERIC_MAP);
        assertEquals(ImmutableList.of("dog", "cat"), (List) result.get("queryTerms"));
    }
}
