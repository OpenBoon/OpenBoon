package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ApiKey;
import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.Asset.Asset;
import com.zorroa.zmlp.sdk.domain.Asset.AssetSpec;
import com.zorroa.zmlp.sdk.domain.Asset.BatchCreateAssetRequest;
import com.zorroa.zmlp.sdk.domain.Asset.BatchCreateAssetResponse;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AssetAppTests extends AbstractAppTest {

    AssetApp assetApp;

    @Before
    public void setup() {

        ApiKey key = new ApiKey(UUID.randomUUID(), "1234");
        assetApp = new AssetApp(
                new ZmlpClient(key, webServer.url("/").toString()));
    }

    @Test
    public void testImportFiles() {

        webServer.enqueue(new MockResponse().setBody(Json.asJson(getImportFilesMock())));

        AssetSpec fileImport = new AssetSpec("gs://zorroa-dev-data/image/pluto.png");

        BatchCreateAssetRequest batchCreateAssetRequest = new BatchCreateAssetRequest();
        batchCreateAssetRequest.setAssets(Arrays.asList(fileImport));

        BatchCreateAssetResponse batchCreateAssetResponse = assetApp.importFiles(batchCreateAssetRequest);

        assertEquals(batchCreateAssetResponse.getStatus().get(0).getAssetId(), "abc123");
        assertEquals(batchCreateAssetResponse.getStatus().get(0).getFailed(), false);
    }

    @Test
    public void testGetById(){

        webServer.enqueue(new MockResponse().setBody(Json.asJson(getGetByIdMock())));

        Asset asset = assetApp.getById("abc123");

        assertNotNull(asset.getId());
        assertNotNull(asset.getDocument());
    }

    @Test
    public void testUploadAssets(){

        webServer.enqueue(new MockResponse().setBody(Json.asJson(getUploadAssetsMock())));

        List<AssetSpec> assetSpecList = Arrays.asList(new AssetSpec("../../../zorroa-test-data/images/set01/toucan.jpg"));
        BatchCreateAssetResponse response = assetApp.uploadFiles(assetSpecList);

        assertEquals(response.getStatus().get(0).getAssetId(), "abc123");
    }

    // Mocks
    private Map getMockSearchResult() {
        Map mock = new HashMap();
        mock.put("took", 4);
        mock.put("timed_out", false);

        List hitsMock = new ArrayList();
        Map hit1 = new HashMap();
        hit1.put("_index", "litvqrkus86sna2w");
        hit1.put("_type", "asset");
        hit1.put("_id", "dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg");
        hit1.put("_score", 0.2876821);
        Map hit1_Source = new HashMap();
        Map hit1Source = new HashMap();
        hit1Source.put("path", "https://i.imgur.com/SSN26nN.jpg");
        hit1_Source.put("source", hit1Source);
        hit1.put("_source", hit1_Source);
        hitsMock.add(hit1);

        Map hit2 = new HashMap();
        hit2.put("_index", "litvqrkus86sna2w");
        hit2.put("_type", "asset");
        hit2.put("_id", "dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg");
        hit2.put("_score", 0.2876821);
        Map hit2_Source = new HashMap();
        Map hit2Source = new HashMap();
        hit2Source.put("path", "https://i.imgur.com/foo.jpg");
        hit2_Source.put("source", hit2Source);
        hit2.put("_source", hit2_Source);
        hitsMock.add(hit2);

        return mock;
    }

    private Map getImportFilesMock() {
        Map mock = new HashMap();

        List statusMockList = new ArrayList();
        Map statusMockMap1 = new HashMap();
        statusMockMap1.put("assetId", "abc123");
        statusMockMap1.put("failed", false);
        statusMockList.add(statusMockMap1);
        mock.put("status", statusMockList);

        List assetsMockList = new ArrayList();
        Map assetsMockMap1 = new HashMap();
        assetsMockMap1.put("id", "abc123");
        Map documentMockMap = new HashMap();
        Map sourceMockMap = new HashMap();
        sourceMockMap.put("path", "gs://zorroa-dev-data/image/pluto.png");
        documentMockMap.put("source", sourceMockMap);
        assetsMockMap1.put("document", documentMockMap);
        assetsMockList.add(assetsMockMap1);
        mock.put("assets", assetsMockList);

        return mock;

    }

    private Map getGetByIdMock(){
        Map mock = new HashMap();

        mock.put("id", "abc123");
        Map documentMock = new HashMap();
        Map sourceMock = new HashMap();
        sourceMock.put("path", "gs://zorroa-dev-data/image/pluto.png");
        documentMock.put("source", sourceMock);
        mock.put("document", documentMock);

        return mock;
    }

    private Map getUploadAssetsMock(){
        Map mock = new HashMap();
        List<Map> statusListMock = new ArrayList();
        Map statusMock = new HashMap();
        statusMock.put("assetId","abc123");
        statusMock.put("failed", false);
        statusListMock.add(statusMock);
        mock.put("status", statusListMock);

        Map sourceMock = new HashMap();
        sourceMock.put("path", "zmlp:///abc123/source/toucan.jpg");
        Map documentMock = new HashMap();
        documentMock.put("source", sourceMock);

        List<Map> assetsListMock = new ArrayList();
        Map assetsMock = new HashMap();
        assetsMock.put("id", "abc123");
        assetsMock.put("document", documentMock);
        assetsListMock.add(assetsMock);

        mock.put("assets", assetsListMock);

        return mock;

    }
}
