package domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Utils.class)
@PowerMockIgnore({"javax.crypto.*" })
public class PixmlDataSourceAppTests {

    Map keyDict;
    PixmlApp app;

    @Mock
    DataSourceApp mockedDsApp;

    @Mock
    DataSource mockedDs;

    ObjectMapper mapper;

    @Before
    public void setup() {
        //This is not a valid key
        keyDict = new HashMap();
        keyDict.put("projectId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("keyId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("sharedKey", "test123test135");

        app = new PixmlApp(keyDict);
        mapper = new ObjectMapper();
    }

    @Test
    public void createDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("id", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        value.put("name", "test");
        value.put("uri", "gs://test/test");
        value.put("file_types", Arrays.asList("jpg"));
        value.put("analysis", Arrays.asList("google-ocr"));
        DataSource mockedDs = new DataSource(value);


        //Mocking static method
        PowerMockito.mockStatic(Utils.class);
        BDDMockito.given(Utils.executeHttpRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
                .willReturn(mapper.writeValueAsString(value));

        //run real method with static mocked method with http request inside
        DataSource ds = app.dataSource.createDataSource("test", "gs://test/test", null, null, null);

        assertEquals(value.get("id"), ds.getId());
        assertEquals(value.get("name"), ds.getName());
        assertEquals(value.get("uri"), ds.getUri());
        assertEquals(ds.getFileTypes(), Arrays.asList("jpg"));
        assertEquals(ds.getAnalysis(), Arrays.asList("google-ocr"));

    }

    @Test
    public void getDataSource() {
        fail();

    }

    @Test
    public void importDataSource() {
        fail();

    }

    @Test
    public void updateCredentials() {
        fail();

    }

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
