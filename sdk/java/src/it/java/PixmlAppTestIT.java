import com.fasterxml.jackson.databind.ObjectMapper;
import domain.DataSource;
import domain.PixmlApp;
import domain.Utils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PixmlAppTestIT {

    PixmlApp pixmlApp;
    String server = "http://localhost:8080";
    String projectName = "ProjectZERO";
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    ObjectMapper mapper;
    DataSource dsTest;


    @BeforeAll
    public void setup() throws ReflectiveOperationException {
        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        Utils.updateEnvVariables("PIXML_APIKEY_FILE", "../../dev/config/keys/inception-key.json");
        Utils.updateEnvVariables("PIXML_SERVER", server);
        pixmlApp = new PixmlApp();
        mapper = new ObjectMapper();

    }

    @Test
    @Order(1)
    public void testCreateAppFromEnvFile() throws IOException, InterruptedException {

        Map requestParams = new HashMap();
        requestParams.put("name", projectName);
        requestParams.put("projectId", projectId);

        Map post = pixmlApp.getPixmlClient().post("/api/v1/projects", requestParams);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.getPixmlClient());
        assertNotNull(pixmlApp.getPixmlClient().getApiKey());
        assertNotNull(pixmlApp.getPixmlClient().headers());

        assertEquals(post.get("id"), projectId.toString());
        assertEquals(post.get("name"), projectName);
    }


    @Test
    @Order(2)
    public void testCreateRepeatedAppFromEnvFile() {

        Map requestParams = new HashMap();
        requestParams.put("name", projectName);
        requestParams.put("projectId", projectId);

        assertThrows(IOException.class, () -> pixmlApp.getPixmlClient().post("/api/v1/projects", requestParams));
    }

    @Test
    @Order(3)
    public void testRetrieveProject() throws IOException, InterruptedException {

        List<UUID> strings = Arrays.asList(projectId);
        Map searchFilter = new HashMap();
        searchFilter.put("ids", strings);

        Map response = pixmlApp.getPixmlClient().post("/api/v1/projects/_findOne", searchFilter);

        assertEquals(response.get("id"), projectId.toString());
        assertEquals(response.get("name"), projectName);
        assertEquals(response.get("actorCreated"), "admin-key");
        assertEquals(response.get("actorModified"), "admin-key");
    }

    @Test
    @Order(4)
    public void createDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("name", "test");
        value.put("uri", "gs://test/test");

        dsTest = pixmlApp.getDataSourceApp().createDataSource("test", "gs://test/test", null, null, null);

        assertEquals(value.get("name"), dsTest.getName());
        assertEquals(value.get("uri"), dsTest.getUri());

    }


    @Test
    @Order(5)
    public void getDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("name", "test");
        value.put("uri", "gs://test/test");

        //run real method with static mocked method with http request inside
        DataSource ds = pixmlApp.getDataSourceApp().getDataSource("test");

        assertEquals(value.get("name"), ds.getName());
        assertEquals(value.get("uri"), ds.getUri());

    }

    @Test
    @Order(6)
    public void importDataSource() throws IOException, InterruptedException {

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", dsTest.getId());
        DataSource ds = new DataSource(dataSourceParam);

        Map response = pixmlApp.getDataSourceApp().importDataSource(ds);

        assertEquals(response.get("dataSourceId"), dsTest.getId().toString());
        assert (((String) response.get("name")).contains(dsTest.getName()));
    }

    @Test
    @Order(7)
    public void updateCredentials() throws IOException, InterruptedException {

        Map status = this.pixmlApp.getDataSourceApp().updateCredentials(dsTest, "ABC123");

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", projectId);
        DataSource ds = new DataSource(dataSourceParam);

        assertEquals(status.get("op"), "update");
        assertEquals(status.get("id"), dsTest.getId().toString());
    }

    @Test
    @Order(8)
    public void testDeleteApp() throws IOException, InterruptedException {

        Map post = pixmlApp.getPixmlClient().delete("/api/v1/projects", projectId);

        assertEquals(true, (Boolean) post.get("success"));
        assertEquals("delete", post.get("op"));
        assertEquals("projects", post.get("type"));
        assertEquals("00000000-0000-0000-0000-000000000000", post.get("id"));

    }
}
