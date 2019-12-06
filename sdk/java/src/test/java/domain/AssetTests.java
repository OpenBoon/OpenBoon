package domain;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AssetTests {

    List<Map<String, Object>> testFiles;

    @Before
    public void setup() {

        testFiles = new ArrayList();

        Map<String, Object> file1 = new HashMap<>();
        file1.put("assetId", "123");
        file1.put("category", "proxy");
        file1.put("name", "proxy_200x200.jpg");
        file1.put("mimetype", "image/jpeg");

        Map<String, Object> attrsFile1 = new HashMap<>();
        attrsFile1.put("width", 200);
        attrsFile1.put("height", 200);

        file1.put("attrs", attrsFile1);


        testFiles.add(file1);

    }

    @Test
    public void testGetFilesFilterName() throws Exception {

        Map params = new HashMap();
        params.put("id", 123);
        Asset asset = new Asset(params);
        asset.setAttr("files", testFiles);

        String param1[] = {"proxy_200x200.jpg"};

        List files1 = asset.getFilesByName(param1);
        List files2 = asset.getFilesByName(param1[0]);
        List files3 = asset.getFilesByName("spok");
        List files4 = asset.getFilesByName(null);

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 1);
        assertEquals(files3.size(), 0);
        assertEquals(files4.size(), 0);
    }

    @Test
    public void testGetFilesFilterCategory() throws Exception {
        Map params = new HashMap();
        params.put("id", 123);
        Asset asset = new Asset(params);
        asset.setAttr("files", this.testFiles);

        String param1[] = {"proxy"};
        List files1 = asset.getFilesByCategory(param1);
        List files2 = asset.getFilesByCategory(param1[0]);
        List files3 = asset.getFilesByCategory(null);
        List files4 = asset.getFilesByName("face");

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 1);
        assertEquals(files3.size(), 0);
        assertEquals(files4.size(), 0);

    }

    @Test
    public void testGetFilesFilterMimetype() throws Exception {
        Map params = new HashMap();
        params.put("id", 123);
        Asset asset = new Asset(params);
        asset.setAttr("files", this.testFiles);

        String param1[] = {"image/jpeg"};
        List files1 = asset.getFilesByMimetype(param1[0]);

        String param2[] = {"image/jpeg", "video/mp4"};
        List files2 = asset.getFilesByMimetype(param2);

        String param3[] = {"video/mp4"};
        List files3 = asset.getFilesByMimetype(param3[0]);
        List files4 = asset.getFilesByMimetype(null);

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 1);
        assertEquals(files3.size(), 0);
        assertEquals(files4.size(), 0);

    }

    @Test
    public void getFilesByExtension() throws Exception {

        Map params = new HashMap();
        params.put("id", 123);
        Asset asset = new Asset(params);
        asset.setAttr("files", this.testFiles);

        String param1[] = {"jpg"};
        List files1 = asset.getFilesByExtension(param1);

        String param2[] = {"jpg","png"};
        List files2 = asset.getFilesByExtension(param2);

        String param3[] = {"png"};
        List files3 = asset.getFilesByExtension(param3);
        List files4 = asset.getFilesByExtension(null);

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 1);
        assertEquals(files3.size(), 0);
        assertEquals(files4.size(), 0);

    }

    @Test
    public void getFilesByAttrs() throws Exception {
        Map params = new HashMap();
        params.put("id", 123);
        Asset asset = new Asset(params);
        asset.setAttr("files", this.testFiles);

        Map param1 = new HashMap();
        param1.put("width",200);

        Map param2 = new HashMap();
        param2.put("width",200);
        param2.put("height",100);


        List files1 = asset.getFilesByAttrs(param1);
        List files2 = asset.getFilesByAttrs(param2);

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 0);
    }

    @Test
    public void getFilesByAll() throws Exception {
        fail();
    }

    @Test
    public void testEquality() throws Exception {
        fail();
    }


    /*
        def test_get_files_by_attrs(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(attrs={"width": 200}))
        assert 0 == len(asset.get_files(attrs={"width": 200, "height": 100}))

    def test_get_files_by_all(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(mimetype="image/jpeg",
                                        extension=["png", "jpg"],
                                        attrs={"width": 200}))

    def test_equal(self):
        assert Asset({"id": "123"}) == Asset({"id": "123"})
     */

}
