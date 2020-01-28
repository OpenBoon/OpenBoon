package com.zorroa.zmlp.client.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.Json;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.PagedList;
import com.zorroa.zmlp.client.domain.asset.*;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
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
    public void testSearchElement() {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        Map elementQueryTerms = new HashMap();
        elementQueryTerms.put("element.labels", "cat");

        PagedList<Asset> searchResult = assetApp.search(elementQueryTerms);
        Asset asset = searchResult.get(0);
        String path = asset.getAttr("source.path");

        assertEquals("https://i.imgur.com/SSN26nN.jpg", path);
    }

    @Test
    public void testSearchWrapped() {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        Map search = new HashMap();
        search.put("match_all", new HashMap());

        PagedList<Asset> searchResult = assetApp.search(search);

        Asset asset = searchResult.get(0);
        String nestedValue = asset.getAttr("source.nestedSource.nestedKey");
        String path = asset.getAttr("source.path");

        assertEquals(2, searchResult.getList().size());
        assertEquals("nestedValue", nestedValue);
        assertEquals("https://i.imgur.com/SSN26nN.jpg", path);
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
        asset1.setAttr("update.attr1","updatedValue");
        Asset asset2 = assetApp.getById("123abc");
        asset2.setAttr("update.attr2", "updatedAttribute");

        List<Asset> assetList = Arrays.asList(asset1, asset2);

        Map elasticSearchMap = assetApp.batchUpdate(assetList);

        assertEquals("updated", elasticSearchMap.get("result"));
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

    private String getMockData(String name) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/" + name + ".json")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to find mock data: " + name, e);
        }
    }
}
