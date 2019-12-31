package com.zorroa.zmlp.sdk.domain;

import com.zorroa.zmlp.sdk.domain.Asset.Asset;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssetTests {

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

    private Asset getTestAsset() {
        Map document = new HashMap();
        document.put("files", getTestFiles());

        Asset asset = new Asset("123", document);
        return asset;
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
}
