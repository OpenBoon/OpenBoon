package com.zorroa.zmlp.client.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.Json;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.asset.*;
import com.zorroa.zmlp.client.domain.similarity.SimilaritySearch;
import okhttp3.mockwebserver.MockResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AssetAppTests extends AbstractAppTests {

    AssetApp assetApp;

    @Before
    public void setup() {

        ApiKey key = new ApiKey(UUID.randomUUID().toString(), "1234");
        assetApp = new AssetApp(
                new ZmlpClient(key, webServer.url("/").toString()));
    }

    @Test
    public void testImportFiles() {

        webServer.enqueue(new MockResponse().setBody(getImportFilesMock()));

        AssetCreateBuilder assetCreateBuilder = new AssetCreateBuilder()
                .addAsset(new AssetSpec("gs://zorroa-dev-data/image/pluto.png"))
                .addAsset("gs://zorroa-dev-data/image/earth.png")
                .setAnalyze(false);

        BatchCreateAssetResponse batchCreateAssetResponse = assetApp.importFiles(assetCreateBuilder);

        assertEquals("abc123", batchCreateAssetResponse.getStatus().get(0).getAssetId());
        assertEquals(false, batchCreateAssetResponse.getStatus().get(0).getFailed());
    }

    @Test
    public void testGetById() {

        webServer.enqueue(new MockResponse().setBody(getGetByIdMock()));

        Asset asset = assetApp.getById("abc123");

        assertNotNull(asset.getId());
        assertNotNull(asset.getDocument());
    }

    @Test
    public void testUploadAssets() {

        webServer.enqueue(new MockResponse().setBody(getUploadAssetsMock()));

        List<AssetSpec> assetSpecList = Arrays.asList(new AssetSpec("src/test/resources/toucan.jpg"));
        BatchCreateAssetResponse response = assetApp.uploadFiles(assetSpecList);

        assertEquals("abc123", response.getStatus().get(0).getAssetId());
    }

    @Test
    public void testUploadAssetsAsArray() {

        webServer.enqueue(new MockResponse().setBody(getUploadAssetsMock()));

        BatchCreateAssetResponse response = assetApp.uploadFiles(new AssetSpec("src/test/resources/toucan.jpg"));

        assertEquals("abc123", response.getStatus().get(0).getAssetId());
    }

    @Test
    public void testUploadAssetsAsAssetBatch() {

        webServer.enqueue(new MockResponse().setBody(getUploadAssetsMock()));

        BatchAssetSpec batchAssetSpec = new BatchAssetSpec()
                .addAsset(new AssetSpec("src/test/resources/toucan.jpg"));

        BatchCreateAssetResponse response = assetApp.uploadFiles(batchAssetSpec);

        assertEquals("abc123", response.getStatus().get(0).getAssetId());
    }

    @Test
    public void testFileCrawlerByType() throws IOException {
        BatchUploadFileCrawler batchUploadFileCrawler = new BatchUploadFileCrawler("./src/test/resources/")
                .addFileType("jpg");
        List<Path> filter = batchUploadFileCrawler.filter();

        assertEquals(1,filter.size());
    }

    @Test
    public void testFileCrawlerByMimetype() throws IOException {
        BatchUploadFileCrawler batchUploadFileCrawler = new BatchUploadFileCrawler("./src/test/resources/")
                .addMimeType("image/jpeg");
        List<Path> filter = batchUploadFileCrawler.filter();

        assertEquals(1,filter.size());
    }

    @Test
    public void testSearchRaw() {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        Map search = new HashMap();
        search.put("match_all", new HashMap());

        Map searchResult = assetApp.rawSearch(search);
        JsonNode jsonNode = Json.mapper.valueToTree(searchResult);

        String path = jsonNode.get("hits").get("hits").get(0).get("_source").get("source").get("path").asText();
        assertEquals("https://i.imgur.com/SSN26nN.jpg", path);
    }

    @Test
    public void testSearchElasticSearch() {
        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("source.filename", "dog.jpg"));

        AssetSearchResult search = assetApp.search(searchSourceBuilder);

        assertEquals(2, search.assets().size());
        assertEquals(2, search.size());
        assertEquals(100, search.totalSize());
        assertEquals(getMockSearchResult(), Json.asPrettyJson(search.rawResponse()));
    }

    @Test
    public void testSimilarityElasticSearch() {
        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(
                new SimilaritySearch("field")
                        .add("HASH-HASH-HASH-HASH-HASH-HASH-HASH"));

        /*
        {
           "query":{
              "similarity":{
                 "field":"[{weight=1.0, hash=HASH-HASH-HASH-HASH-HASH-HASH-HASH}]",
                 "boost":1.0
              }
           }
        }
         */
        AssetSearchResult search = assetApp.search(searchSourceBuilder);

        assertEquals(2, search.assets().size());
        assertEquals(2, search.size());
        assertEquals(100, search.totalSize());
        assertEquals(getMockSearchResult(), Json.asPrettyJson(search.rawResponse()));
    }

    @Test
    public void testSimilarityMultipleFilters() {
        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(
                new SimilaritySearch("batchField")
                        .add("HASH11111111111111111")
                        .add("HASH22222222222222222222", 2.0)
        );

        /*
        {
           "query":{
              "similarity":{
                 "batchField":"[{weight=1.0, hash=HASH11111111111111111}, {weight=1.0, hash=HASH22222222222222222222}]",
              }
           }
        }
         */

        AssetSearchResult search = assetApp.search(searchSourceBuilder);

        assertEquals(2, search.assets().size());
        assertEquals(2, search.size());
        assertEquals(100, search.totalSize());
        assertEquals(getMockSearchResult(), Json.asPrettyJson(search.rawResponse()));
    }


    @Test
    public void testSimilarityAddAll() {
        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        List<Map<String,Object>> filters = new ArrayList();
        Map hash1 = new HashMap();
        hash1.put("hash", "HASH-NUMBER-ONE");
        hash1.put( "weight", 3.8);
        filters.add(hash1);

        Map hash2 = new HashMap();
        hash2.put("hash", "HASH-NUMBER-TWO");
        hash2.put( "weight", 1);
        filters.add(hash2);

        searchSourceBuilder.query(
                new SimilaritySearch("batchField")
                        .addAll(filters));

        /*
        {
           "query":{
              "similarity":{
                 "batchField":"[{weight=3.8, hash=HASH-NUMBER-ONE}, {weight=1, hash=HASH-NUMBER-TWO}]",
                 "boost":1.0
              }
           }
        }
         */

        AssetSearchResult search = assetApp.search(searchSourceBuilder);

        assertEquals(2, search.assets().size());
        assertEquals(2, search.size());
        assertEquals(100, search.totalSize());
        assertEquals(getMockSearchResult(), Json.asPrettyJson(search.rawResponse()));
    }

    @Test
    public void testSearchProperties() throws JsonProcessingException {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        String query = "{\"query\": {\"term\": {\"source.filename\": \"dog.jpg\"}}}";
        Map elementQueryTerms = Json.mapper.readValue(query, Map.class);


        AssetSearchResult searchResult = assetApp.search(elementQueryTerms);

        assertEquals(2, searchResult.assets().size());
        assertEquals(2, searchResult.size());
        assertEquals(100, searchResult.totalSize());
        assertEquals(getMockSearchResult(), Json.asPrettyJson(searchResult.rawResponse()));
    }

    @Test
    public void testSearchNextPage() throws JsonProcessingException {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));
        String secondPageMock = "{\"hits\":{\"hits\":[]}}";
        webServer.enqueue(new MockResponse().setBody(secondPageMock));


        String query = "{\"query\": {\"term\": {\"source.filename\": \"dog.jpg\"}}}";
        Map elementQueryTerms = Json.mapper.readValue(query, Map.class);


        AssetSearchResult searchResult = assetApp.search(elementQueryTerms);
        AssetSearchResult secondPage = searchResult.nextPage();

        assertEquals(secondPageMock, Json.asJson(secondPage.rawResponse()));
    }


    @Test
    public void testScrollSearch() throws JsonProcessingException {
        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));
        // Second/last Page Mock
        webServer.enqueue(new MockResponse().setBody("{\"hits\":{\"hits\":[]}}"));
        //Delete response
        webServer.enqueue(new MockResponse().setBody("{}"));

        String query = "{\"query\": {\"term\": {\"source.filename\": \"dog.jpg\"}}}";
        Map elementQueryTerms = Json.mapper.readValue(query, Map.class);

        AssetSearchScroller searchScroller = assetApp.scrollSearch(elementQueryTerms, null);

        int size = 0;
        while (searchScroller.hasNext()) {
            size++;
            searchScroller.next();
        }

        assertEquals(1, size);
    }

    @Test
    public void testScrollElasticSearch() throws JsonProcessingException {
        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));
        // Second/last Page Mock
        webServer.enqueue(new MockResponse().setBody("{\"hits\":{\"hits\":[]}}"));
        //Delete response
        webServer.enqueue(new MockResponse().setBody("{}"));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("source.filename", "dog.jpg"));

        AssetSearchScroller searchScroller = assetApp.scrollSearch(searchSourceBuilder, null);

        int size = 0;
        while (searchScroller.hasNext()) {
            size++;
            searchScroller.next();
        }

        assertEquals(1, size);
    }

    @Test
    public void testIndex() throws IOException {
        webServer.enqueue(new MockResponse().setBody(getGetByIdMock()));
        webServer.enqueue(new MockResponse().setBody(getMockIndex()));

        Asset asset = assetApp.getById("abc123");

        asset.setAttr("aux.my_field", 1000);
        asset.setAttr("aux.other_field", "fieldValue");
        Map indexedAsset = assetApp.index(asset);

        assertEquals(Json.mapper.readValue(getMockIndex().getBytes(), Map.class), indexedAsset);
    }

    @Test
    public void testAssetUpdate() {
        webServer.enqueue(new MockResponse().setBody(getGetByIdMock()));
        webServer.enqueue(new MockResponse().setBody(getMockUpdate()));

        Asset asset = assetApp.getById("abc123");

        Map<String, Object> newDocument = new HashMap();
        Map<String, Object> aux = new HashMap();
        aux.put("captain", "kirk");

        newDocument.put("aux", aux);

        Map elObject = assetApp.update(asset.getId(), newDocument);

        assertEquals("updated", elObject.get("result"));
    }

    @Test
    public void testBatchIndex() {

        webServer.enqueue(new MockResponse().setBody(getGetByIdMock()));
        webServer.enqueue(new MockResponse().setBody(getGetByIdMock2()));
        webServer.enqueue(new MockResponse().setBody(getMockUpdate()));

        Asset asset1 = assetApp.getById("abc123");
        Asset asset2 = assetApp.getById("123abc");

        List<Asset> assetList = Arrays.asList(asset1, asset2);

        Map elasticSearchMap = assetApp.batchIndex(assetList);

        assertEquals("updated", elasticSearchMap.get("result"));
    }

    @Test
    public void testBatchUpdate() {

        webServer.enqueue(new MockResponse().setBody(getGetByIdMock()));
        webServer.enqueue(new MockResponse().setBody(getGetByIdMock2()));
        webServer.enqueue(new MockResponse().setBody(getMockUpdate()));

        Asset asset1 = assetApp.getById("abc123");
        asset1.setAttr("update.attr1", "updatedValue");
        Asset asset2 = assetApp.getById("123abc");
        asset2.setAttr("update.attr2", "updatedAttribute");

        List<Asset> assetList = Arrays.asList(asset1, asset2);

        Map elasticSearchMap = assetApp.batchUpdate(assetList);

        assertEquals("updated", elasticSearchMap.get("result"));
    }

    @Test
    public void testDelete() {
        webServer.enqueue(new MockResponse().setBody(getGetByIdMock()));
        webServer.enqueue(new MockResponse().setBody(getMockDelete()));

        Asset asset = assetApp.getById("abc123");

        Map deleteES = assetApp.delete(asset);

        assertEquals("deleted", deleteES.get("result"));
    }

    @Test
    public void testDeleteByQuery() throws JsonProcessingException {
        webServer.enqueue(new MockResponse().setBody(getMockDelete()));

        Map deleteES = assetApp.deleteByQuery(getRequestMockDeleteByQuery());

        assertEquals("deleted", deleteES.get("result"));
    }

    // Mocks
    private String getImportFilesMock() {
        return getMockData("mock-import-files");
    }

    private String getGetByIdMock() {
        return getMockData("mock-get-by-id");
    }

    private String getGetByIdMock2() {
        return getMockData("mock-get-by-id-2");
    }

    private String getUploadAssetsMock() {
        return getMockData("mock-upload-assets");
    }

    private String getMockSearchResult() {
        return getMockData("mock-search-result");
    }

    private String getMockIndex() {
        return getMockData("mock-index-assets");
    }

    private String getMockUpdate() {
        return getMockData("mock-update-asset");
    }

    private String getMockDelete() {
        return getMockData("mock-delete");
    }

    private String getRequestMockDeleteByQuery() {
        return getMockData("mock-delete-by-query-request");
    }

    private String getMockData(String name) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/" + name + ".json")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to find mock data: " + name, e);
        }
    }
}
