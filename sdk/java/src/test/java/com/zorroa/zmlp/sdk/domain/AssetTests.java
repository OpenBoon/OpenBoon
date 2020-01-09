package com.zorroa.zmlp.sdk.domain;

import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.domain.asset.Asset;
import com.zorroa.zmlp.sdk.domain.asset.AssetFilesFilter;
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

        List files1 = asset.getFiles(new AssetFilesFilter().addName("proxy_200x200.jpg"));
        List files2 = asset.getFiles(new AssetFilesFilter().addName("spock"));

        assertEquals(1, files1.size());
        assertEquals(0, files2.size());
    }

    @Test
    public void testAssetGetFilesFilterCategory() {
        Asset asset = getTestAsset();

        AssetFilesFilter proxy = new AssetFilesFilter().addCategory("proxy");
        AssetFilesFilter face = new AssetFilesFilter().addCategory("face");

        assertEquals(1, asset.getFiles(proxy).size());
        assertEquals(0, asset.getFiles(face).size());
    }

    @Test
    public void testAssetGetFilesFilterMimetype() {
        Asset asset = getTestAsset();

        AssetFilesFilter image = new AssetFilesFilter().addMimetype("image/jpeg");
        AssetFilesFilter video = new AssetFilesFilter().addMimetype("video/mp4");
        AssetFilesFilter both = new AssetFilesFilter().addMimetype("video/mp4").addMimetype("image/jpeg");

        assertEquals(1, asset.getFiles(both).size());
        assertEquals(1, asset.getFiles(image).size());
        assertEquals(0, asset.getFiles(video).size());
    }

    @Test
    public void testAssetGetFilesFilterByExtension() {
        Asset asset = getTestAsset();

        AssetFilesFilter png = new AssetFilesFilter().addExtension("png");
        AssetFilesFilter jpg = new AssetFilesFilter().addExtension("jpg");
        AssetFilesFilter both = new AssetFilesFilter().addExtension("jpg").addExtension("png");

        assertEquals(1, asset.getFiles(both).size());
        assertEquals(1, asset.getFiles(jpg).size());
        assertEquals(0, asset.getFiles(png).size());
    }

    @Test
    public void testAssetGetFilesFilterByAttrs() {

        Asset asset = getTestAsset();

        AssetFilesFilter width = new AssetFilesFilter().addAttr("width", 200);
        AssetFilesFilter widthHeight = new AssetFilesFilter().addAttr("width", 200).addAttr("height", 100);

        assertEquals(1, asset.getFiles(width).size());
        assertEquals(0, asset.getFiles(widthHeight).size());
    }

    @Test
    public void testAssetGetFilesFilterByAttrsKeys() {

        Asset asset = getTestAsset();

        AssetFilesFilter width = new AssetFilesFilter().addAttrKey("width");
        AssetFilesFilter kirk = new AssetFilesFilter().addAttrKey("kirk");

        assertEquals(1, asset.getFiles(width).size());
        assertEquals(0, asset.getFiles(kirk).size());
    }

    @Test
    public void testGetAttribute() {

        Asset asset = getNestedAttributesAssetMock();

        String attr = asset.getAttr("path");

        assertEquals(attr, "https://i.imgur.com/SSN26nN.jpg");
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
        assertTrue(asset.attrExists("path"));
        assertFalse(asset.attrExists("duck"));
        assertTrue(asset.attrExists("nestedSource.nestedKey"));
        assertFalse(asset.attrExists("notPresentKey.AlsoNotPresentKey"));
    }

    @Test
    public void testRemoveAttribute() {
        Asset asset = getNestedAttributesAssetMock();

        assert (asset.attrExists("path"));

        asset.removeAttr("path");

        assertFalse(asset.attrExists("path"));
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
