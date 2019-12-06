package domain;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

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
        List files3 = asset.getFilesByName("spok", null, null, null, null);

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 1);
        assertEquals(files3.size(), 0);
    }

    @Test
    public void testGetFiltesFilterCategory() throws Exception {
        Map params = new HashMap();
        params.put("id", 123);
        Asset asset = new Asset(params);
        asset.setAttr("files", this.testFiles);

        String param1[] = {"proxy"};
        List files1 = asset.getFilesByCategory(param1);
        List files2 = asset.getFilesByCategory(param1[0]);
        List files3 = asset.getFilesByName("face");

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 1);
        assertEquals(files3.size(), 0);

    }

    @Test
    public void testGetFiltesFilterMimetype() throws Exception {
        Map params = new HashMap();
        params.put("id", 123);
        Asset asset = new Asset(params);
        asset.setAttr("files", this.testFiles);

        String param1[] = {"image/jpeg"};
        List files1 = asset.getFilesByCategory(param1[0]);

        String param2[] = {"image/jpeg", "video/mp4"};
        List files2 = asset.getFilesByCategory(param2);

        String param3[] = {"video/mp4"};
        List files3 = asset.getFilesByName(param3[0]);

        assertEquals(files1.size(), 1);
        assertEquals(files2.size(), 1);
        assertEquals(files3.size(), 0);

    }




    /*
    def test_get_files_by_extension(self):
        asset = Asset({"id": "123"})
        asset.set_attr("files", self.test_files)

        assert 1 == len(asset.get_files(extension="jpg"))
        assert 0 == len(asset.get_files(extension="png"))
        assert 1 == len(asset.get_files(extension=["png", "jpg"]))
     */
}
