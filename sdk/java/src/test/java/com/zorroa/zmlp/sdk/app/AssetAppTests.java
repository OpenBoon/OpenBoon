package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ApiKey;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.Asset;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssetAppTests extends AbstractAppTest {

    AssetApp assetApp;

    @Before
    public void setup() {

        ApiKey key = new ApiKey(UUID.randomUUID(), "1234");
        assetApp = new AssetApp(
                new ZmlpClient(key, webServer.url("/").toString()));
    }

    // Asset Tests
    @Test
    public void testAssetGetFilesFilterName() {
        Asset asset = getTestAsset();

        String array[] = {"proxy_200x200.jpg"};
        assertEquals(asset.getFilesByName(array).size(), 1);
        assertEquals(asset.getFilesByName("proxy_200x200.jpg").size(), 1);
        assertEquals(asset.getFilesByName("spock").size(), 0);
    }

    @Test
    public void testAssetGetFilesFilterCategory() {
        Asset asset = getTestAsset();

        String array[] = {"proxy"};
        assertEquals(asset.getFilesByCategory(array).size(), 1);
        assertEquals(asset.getFilesByCategory("proxy").size(), 1);
        assertEquals(asset.getFilesByCategory("face").size(), 0);
    }

    @Test
    public void testAssetGetFilesFilterMimetype() {
        Asset asset = getTestAsset();

        String array[] = {"image/", "video/mp4"};
        assertEquals(asset.getFilesByMimetype(array).size(), 1);
        assertEquals(asset.getFilesByMimetype("image/jpeg").size(), 1);
        assertEquals(asset.getFilesByMimetype("video/mp4").size(), 0);
    }

    @Test
    public void testAssetGetFilesFilterByExtension() {
        Asset asset = getTestAsset();

        String array[] = {"png", "jpg"};
        assertEquals(asset.getFilesByExtension(array).size(), 1);
        assertEquals(asset.getFilesByExtension("jpg").size(), 1);
        assertEquals(asset.getFilesByExtension("png").size(), 0);
    }

    @Test
    public void testAssetGetFilesFilterByAttrs() {

        Asset asset = getTestAsset();

        Map attr1 = new HashMap();
        attr1.put("width", 200);

        Map attr2 = new HashMap();
        attr2.put("width", 200);
        attr2.put("height", 100);

        assertEquals(asset.getFilesByAttrs(attr1).size(), 1);
        assertEquals(asset.getFilesByAttrs(attr2).size(), 0);
    }

    @Test
    public void testAssetGetFilesFilterByAttrsKeys() {

        Asset asset = getTestAsset();

        String array1[] = {"width"};
        String array2[] = {"kirk"};

        assertEquals(asset.getFilesByAttrsKey(array1).size(), 1);
        assertEquals(asset.getFilesByAttrsKey("width").size(), 1);
        assertEquals(asset.getFilesByAttrsKey(array2).size(), 0);
    }

    //Asset App Tests
    @Test
    public void testImportFiles() {

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

    private List<Map> getTestFiles() {

        List<Map> testFiles = new ArrayList();

        Map<String, Object> file1 = new HashMap();

        file1.put("category", "proxy");
        file1.put("name", "proxy_200x200.jpg");
        file1.put("mimetype", "image/jpeg");

        Map<String, Object> attrs = new HashMap();
        attrs.put("width", 200);
        attrs.put("height", 200);

        file1.put("attrs", attrs);

        testFiles.add(file1);

        return testFiles;
    }

    private Asset getTestAsset() {
        Asset asset = new Asset("123");
        asset.setDocumentAttr("files", getTestFiles());
        return asset;
    }

    private Map getTestImportFilesMock() {
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

}
