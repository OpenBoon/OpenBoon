package com.zorroa.zmlp.sdk.domain;

import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.domain.asset.Asset;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AssetTests {

    @Test
    public void testAssetGetFilesFilterName() {
        Asset asset = getTestAsset();

        String array[] = {"proxy_200x200.jpg"};
        assertEquals(1, asset.getFilesByName(array).size());
        assertEquals(1, asset.getFilesByName("proxy_200x200.jpg").size());
        assertEquals(0, asset.getFilesByName("spock").size());
    }

    @Test
    public void testAssetGetFilesFilterCategory() {
        Asset asset = getTestAsset();

        String array[] = {"proxy"};
        assertEquals(1, asset.getFilesByCategory(array).size());
        assertEquals(1, asset.getFilesByCategory("proxy").size());
        assertEquals(0, asset.getFilesByCategory("face").size());
    }

    @Test
    public void testAssetGetFilesFilterMimetype() {
        Asset asset = getTestAsset();

        String array[] = {"image/", "video/mp4"};
        assertEquals(1, asset.getFilesByMimetype(array).size());
        assertEquals(1, asset.getFilesByMimetype("image/jpeg").size());
        assertEquals(0, asset.getFilesByMimetype("video/mp4").size());
    }

    @Test
    public void testAssetGetFilesFilterByExtension() {
        Asset asset = getTestAsset();

        String array[] = {"png", "jpg"};
        assertEquals(1, asset.getFilesByExtension(array).size());
        assertEquals(1, asset.getFilesByExtension("jpg").size());
        assertEquals(0, asset.getFilesByExtension("png").size());
    }

    @Test
    public void testAssetGetFilesFilterByAttrs() {

        Asset asset = getTestAsset();

        Map attr1 = new HashMap();
        attr1.put("width", 200);

        Map attr2 = new HashMap();
        attr2.put("width", 200);
        attr2.put("height", 100);

        assertEquals(1, asset.getFilesByAttrs(attr1).size());
        assertEquals(0, asset.getFilesByAttrs(attr2).size());
    }

    @Test
    public void testAssetGetFilesFilterByAttrsKeys() {

        Asset asset = getTestAsset();

        String array1[] = {"width"};
        String array2[] = {"kirk"};

        assertEquals(1, asset.getFilesByAttrsKey(array1).size());
        assertEquals(1, asset.getFilesByAttrsKey("width").size());
        assertEquals(0, asset.getFilesByAttrsKey(array2).size());
    }

    @Test
    public void testGetAttribute() {

        Asset asset = getNestedAttributesAssetMock();

        String attr = asset.getAttr("path");

        assertEquals("https://i.imgur.com/SSN26nN.jpg", attr);
    }

    @Test
    public void testSetAttribute() {

        Asset asset = getNestedAttributesAssetMock();

        asset.setAttr("newKey", "newValue");
        String newValue = asset.getAttr("newKey");

        assertEquals("newValue", newValue);

    }

    @Test
    public void testSetNestedAttribute() {

        Asset asset = getNestedAttributesAssetMock();

        asset.setAttr("nestedKey.newKey", "newValue");
        String newValue = asset.getAttr("nestedKey.newKey", String.class);

        assertEquals("newValue", newValue);
    }

    @Test
    public void testAttributeExists() {
        Asset asset = getNestedAttributesAssetMock();
        assertEquals(true, asset.attrExists("path"));
        assertEquals(false, asset.attrExists("duck"));
        assertEquals(true, asset.attrExists("nestedSource.nestedKey"));
        assertEquals(false, asset.attrExists("notPresentKey.AlsoNotPresentKey"));
    }

    @Test
    public void testRemoveAttribute() {
        Asset asset = getNestedAttributesAssetMock();

        assert (asset.attrExists("path"));

        asset.removeAttr("path");

        assertEquals(false,asset.attrExists("path"));
    }

    //Mocks
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

    private Asset getNestedAttributesAssetMock() {

        Map<String, Object> document = new HashMap();
        document.put("path", "https://i.imgur.com/SSN26nN.jpg");

        Map<String, Object> nestedSourceMap = new HashMap();
        nestedSourceMap.put("nestedKey", "nestedValue");
        document.put("nestedSource", nestedSourceMap);

        Asset asset = new Asset("dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg", document);

        return asset;
    }

}
