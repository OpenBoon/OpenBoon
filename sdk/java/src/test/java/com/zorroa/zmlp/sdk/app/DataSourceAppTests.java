package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ApiKey;
import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.DataSource;
import com.zorroa.zmlp.sdk.domain.DataSourceSpec;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class DataSourceAppTests extends AbstractAppTest {

    DataSourceApp dataSourceApp;

    @Before
    public void setup() {
        ApiKey key = new ApiKey(UUID.randomUUID(), "1234");
        dataSourceApp = new DataSourceApp(
                new ZmlpClient(key, webServer.url("/").toString()));

    }

    @Test
    public void testCreateDataSource() {
        Map<String, Object> body = getDataSourceBody();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        DataSourceSpec dataSourceSpec = new DataSourceSpec();
        dataSourceSpec.setName((String)body.get("name"));
        dataSourceSpec.setUri((String)body.get("uri"));
        dataSourceSpec.setFileTypes((List<String>)body.get("file_types"));
        dataSourceSpec.setAnalysis((List<String>)body.get("analysis"));

        DataSource dataSource = dataSourceApp.createDataSource(dataSourceSpec);

        assertEquals(dataSource.getId().toString(), body.get("id"));
        assertEquals(dataSource.getName(), body.get("name"));
        assertEquals(dataSource.getUri(), body.get("uri"));
        assertEquals(dataSource.getFileTypes(), body.get("file_types"));
        assertEquals(dataSource.getAnalysis(), body.get("analysis"));

    }

    @Test
    public void testGetDataSource(){
        Map<String, Object> body = getDataSourceBody();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        DataSource dataSource = dataSourceApp.getDataSource((String)body.get("id"));

        assertEquals(dataSource.getId().toString(), body.get("id"));
        assertEquals(dataSource.getName(), body.get("name"));
        assertEquals(dataSource.getUri(), body.get("uri"));
        assertEquals(dataSource.getFileTypes(), body.get("file_types"));
        assertEquals(dataSource.getAnalysis(), body.get("analysis"));
    }

    @Test
    public void testImportFiles(){
        Map<String, Object> body = getDataSourceBody();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        DataSource emptyDataSource = new DataSource();
        emptyDataSource.setId(UUID.fromString((String)body.get("id")));

        Map dataSourceMap = dataSourceApp.importDataSource(emptyDataSource);

        assertEquals(dataSourceMap.get("id"), body.get("id"));
        assertEquals(dataSourceMap.get("name"), body.get("name"));
        assertEquals(dataSourceMap.get("uri"), body.get("uri"));
        assertEquals(dataSourceMap.get("file_types"), body.get("file_types"));
        assertEquals(dataSourceMap.get("analysis"), body.get("analysis"));
    }

    @Test
    public void testUpdateCredentials(){

        UUID id = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("type", "DATASOURCE");

        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        DataSource dataSource = new DataSource();
        dataSource.setId(id);

        Map status = dataSourceApp.updateCredentials(dataSource, "UpdatedCredentials");

        assertEquals(status.get("type"),"DATASOURCE");
        assertEquals(status.get("id"),id.toString());
    }

    public Map getDataSourceBody() {
        Map<String, Object> body = new HashMap();
        body.put("id", UUID.randomUUID().toString());
        body.put("name", "test");
        body.put("uri", "gs://test/test");
        body.put("file_types'", Arrays.asList("jpg"));
        body.put("analysis", Arrays.asList("google-ocr"));
        return body;
    }
}





