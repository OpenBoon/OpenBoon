package domain;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PixmlDataSourceAppTests {

    Map keyDict;
    PixmlApp app;

    @Before
    public void setup() {
        //This is not a valid key
        keyDict = new HashMap();
        keyDict.put("projectId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("keyId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("sharedKey", "test123test135");

        app = new PixmlApp(keyDict);
    }

    @Test
    public void createDataSource(){}

    @Test
    public void getDataSource(){}

    @Test
    public void importDataSource(){}

    @Test
    public void updateCredentials(){}

    /*
        @patch.object(PixmlClient, 'post')
    def test_create_datasource(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'uri': 'gs://test/test',
            'file_types': ['jpg'],
            'analysis': ['google-ocr']
        }
        post_patch.return_value = value
        ds = self.app.datasource.create_datasource('test', 'gs://test/test')
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert value['uri'] == ds.uri
        assert ds.file_types == ['jpg']
        assert ds.analysis == ['google-ocr']

    @patch.object(PixmlClient, 'post')
    def test_get_datasource(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'test',
            'uri': 'gs://test/test'
        }
        post_patch.return_value = value
        ds = self.app.datasource.get_datasource('test')
        assert value['id'] == ds.id
        assert value['name'] == ds.name
        assert value['uri'] == ds.uri

    @patch.object(PixmlClient, 'post')
    def test_import_datasource(self, post_patch):
        value = {
            'id': 'A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80',
            'name': 'Import DataSource'
        }
        post_patch.return_value = value
        job = self.app.datasource.import_datasource(DataSource({'id': '123'}))
        assert value['id'] == job["id"]
        assert value['name'] == job["name"]

    @patch.object(PixmlClient, 'put')
    def test_update_credentials(self, put_patch):
        value = {
            'type': 'DATASOURCE',
            'id': 'ABC'
        }
        put_patch.return_value = value
        status = self.app.datasource.update_credentials(DataSource({'id': '123'}), 'ABC123')
        assert status['type'] == 'DATASOURCE'
        assert status['id'] == 'ABC'
     */

}
