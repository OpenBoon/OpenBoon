package domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Utils.class})
@PowerMockIgnore({"javax.crypto.*","javax.net.ssl.*"})
public class PixmlDataSourceAppTests {

    Map keyDict;
    PixmlApp app;

    ObjectMapper mapper;
    OkHttpClient okHttpClient;

    @Before
    public void setup() {
        //This is not a valid key
        keyDict = new HashMap();
        keyDict.put("projectId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("keyId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("sharedKey", "test123test135");

        app = new PixmlApp(keyDict);
        mapper = new ObjectMapper();
        okHttpClient = new OkHttpClient();
    }

    @Test
    public void createDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("id", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        value.put("name", "test");
        value.put("uri", "gs://test/test");
        value.put("file_types", Arrays.asList("jpg"));
        value.put("analysis", Arrays.asList("google-ocr"));

        //Mocking static method
        PowerMockito.mockStatic(Utils.class);
        BDDMockito.given(Utils.executeHttpRequest(Mockito.matches("post"), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
                .willReturn(mapper.writeValueAsString(value));

        //run real method with static mocked method with http request inside
        DataSource ds = app.getDataSourceApp().createDataSource("test", "gs://test/test", null, null, null);

        assertEquals(value.get("id"), ds.getId());
        assertEquals(value.get("name"), ds.getName());
        assertEquals(value.get("uri"), ds.getUri());
        assertEquals(ds.getFileTypes(), Arrays.asList("jpg"));
        assertEquals(ds.getAnalysis(), Arrays.asList("google-ocr"));

    }

    @Test
    public void getDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("id", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        value.put("name", "test");
        value.put("uri", "gs://test/test");

        //Mocking static method
        PowerMockito.mockStatic(Utils.class);

        BDDMockito.given(Utils.executeHttpRequest(Mockito.matches("post"), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
                .willReturn(mapper.writeValueAsString(value));

        //run real method with static mocked method with http request inside
        DataSource ds = app.getDataSourceApp().getDataSource("test");


        assertEquals(value.get("id"), ds.getId());
        assertEquals(value.get("name"), ds.getName());
        assertEquals(value.get("uri"), ds.getUri());

    }

    @Test
    public void importDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();
        value.put("id", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        value.put("name", "Import DataSource");

        //Mocking static method
        PowerMockito.mockStatic(Utils.class);

        BDDMockito.given(Utils.executeHttpRequest(Mockito.matches("post"), Mockito.anyString(), Mockito.anyMap(), Mockito.isNull()))
                .willReturn(mapper.writeValueAsString(value));

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", "123");
        DataSource ds = new DataSource(dataSourceParam);

        Map response = this.app.getDataSourceApp().importDataSource(ds);

        assertEquals(value.get("id"), response.get("id"));
        assertEquals(value.get("name"), response.get("name"));

    }

    @Test
    public void updateCredentials() throws IOException, InterruptedException {


        Map value = new HashMap();
        value.put("type", "DATASOURCE");
        value.put("id", "ABC");

        //Mocking static method
        PowerMockito.mockStatic(Utils.class);

        BDDMockito.given(Utils.executeHttpRequest(Mockito.matches("put"), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
                .willReturn(mapper.writeValueAsString(value));

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", "123");

        DataSource ds = new DataSource(dataSourceParam);
        Map status = this.app.getDataSourceApp().updateCredentials(ds, "ABC123");

        assertEquals(status.get("type"), "DATASOURCE");
        assertEquals(status.get("id"), "ABC");

    }
}
