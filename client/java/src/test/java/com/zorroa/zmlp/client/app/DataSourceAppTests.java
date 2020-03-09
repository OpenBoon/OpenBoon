package com.zorroa.zmlp.client.app;

import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.Json;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.datasource.DataSource;
import com.zorroa.zmlp.client.domain.datasource.DataSourceCredentials;
import com.zorroa.zmlp.client.domain.datasource.DataSourceSpec;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class DataSourceAppTests extends AbstractAppTests {

    DataSourceApp dataSourceApp;

    @Before
    public void setup() {
        ApiKey key = new ApiKey("abcd", "1234");
        dataSourceApp = new DataSourceApp(
                new ZmlpClient(key, webServer.url("/").toString()));

    }

    @Test
    public void testCreateDataSource() {
        Map<String, Object> body = getDataSourceBody();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        DataSourceSpec dataSourceSpec = new DataSourceSpec()
                .setName((String) body.get("name"))
                .setUri((String) body.get("uri"))
                .setFileTypes((List<String>) body.get("file_types"))
                .setAnalysis((List<String>) body.get("analysis"));

        DataSource dataSource = dataSourceApp.createDataSource(dataSourceSpec);

        assertEquals(body.get("id"), dataSource.getId().toString());
        assertEquals(body.get("name"), dataSource.getName());
        assertEquals(body.get("uri"), dataSource.getUri());
        assertEquals(body.get("file_types"), dataSource.getFileTypes());
        assertEquals(body.get("analysis"), dataSource.getAnalysis());

    }

    @Test
    public void testGetDataSource() {
        Map<String, Object> body = getDataSourceBody();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        DataSource dataSource = dataSourceApp.getDataSource((String) body.get("id"));

        assertEquals(body.get("id"), dataSource.getId().toString());
        assertEquals(body.get("name"), dataSource.getName());
        assertEquals(body.get("uri"), dataSource.getUri());
        assertEquals(body.get("file_types"), dataSource.getFileTypes());
        assertEquals(body.get("analysis"), dataSource.getAnalysis());
    }

    @Test
    public void testImportFiles() {
        Map<String, Object> body = getDataSourceBody();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        DataSource dataSource = dataSourceApp.importDataSource((String) body.get("id"));

        assertEquals(body.get("id"), dataSource.getId().toString());
        assertEquals(body.get("name"), dataSource.getName());
        assertEquals(body.get("uri"), dataSource.getUri());
        assertEquals(body.get("file_types"), dataSource.getFileTypes());
        assertEquals(body.get("analysis"), dataSource.getAnalysis());
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





