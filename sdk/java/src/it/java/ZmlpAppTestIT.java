import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorroa.zmlp.sdk.*;
import domain.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZmlpAppTestIT {

    ZmlpApp zmlpApp;
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
        Utils.updateEnvVariables("ZMLP_APIKEY_FILE", "../../dev/config/keys/inception-key.json");
        Utils.updateEnvVariables("ZMLP_SERVER", server);
        zmlpApp = new ZmlpApp();
        mapper = new ObjectMapper();

    }

    @Test
    @Order(1)
    public void testCreateProject() throws IOException, InterruptedException {

        ProjectSpec projectSpec = new ProjectSpec(projectName, projectId);
        Project project = zmlpApp.getZmlpClient().createProject(projectSpec);

        assertEquals(project.getId(), projectId);
        assertEquals(project.getName(), projectName);
    }


    @Test
    @Order(2)
    public void testFailOnCreateRepeatedApp() {

        ProjectSpec projectSpec = new ProjectSpec(projectName, projectId);

        assertThrows(IOException.class, () -> zmlpApp.getZmlpClient().createProject(projectSpec));
    }

    @Test
    @Order(3)
    public void testRetrieveProject() throws IOException, InterruptedException {

        List<UUID> uuidList = Arrays.asList(projectId);
        ProjectFilter projectFilter = new ProjectFilter(uuidList, null, null, null);

        Project project = zmlpApp.getZmlpClient().searchProject(projectFilter);

        assertEquals(project.getId(), projectId);
        assertEquals(project.getName(), projectName);
        assertEquals(project.getActorCreated(), "admin-key");
        assertEquals(project.getActorCreated(), "admin-key");
    }

    @Test
    @Order(4)
    public void testRetrieveAllProjects() throws IOException, InterruptedException {

        ProjectFilter projectFilter = new ProjectFilter(null, null, null, null);
        List<Project> projects = zmlpApp.getZmlpClient().getAllProjects(projectFilter);
        assertEquals(projects.get(0).getId(), projectId);
        assertEquals(projects.get(0).getName(), projectName);
    }

    @Test
    @Order(5)
    public void createDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("name", "test");
        value.put("uri", "gs://test/test");

        dsTest = zmlpApp.getDataSourceApp().createDataSource("test", "gs://test/test", null, null, null);

        assertEquals(value.get("name"), dsTest.getName());
        assertEquals(value.get("uri"), dsTest.getUri());

    }

    @Test
    @Order(6)
    public void getDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("name", "test");
        value.put("uri", "gs://test/test");

        //run real method with static mocked method with http request inside
        DataSource ds = zmlpApp.getDataSourceApp().getDataSource("test");

        assertEquals(value.get("name"), ds.getName());
        assertEquals(value.get("uri"), ds.getUri());

    }

    @Test
    @Order(7)
    public void importDataSource() throws IOException, InterruptedException {

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", dsTest.getId());
        DataSource ds = new DataSource(dataSourceParam);

        Map response = zmlpApp.getDataSourceApp().importDataSource(ds);

        assertEquals(response.get("dataSourceId"), dsTest.getId().toString());
        assert (((String) response.get("name")).contains(dsTest.getName()));
    }

    @Test
    @Order(8)
    public void updateCredentials() throws IOException, InterruptedException {

        Map status = this.zmlpApp.getDataSourceApp().updateCredentials(dsTest, "ABC123");

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", projectId);
        DataSource ds = new DataSource(dataSourceParam);

        assertEquals(status.get("op"), "update");
        assertEquals(status.get("id"), dsTest.getId().toString());
    }

    @Test
    @Order(9)
    public void testDeleteApp() throws IOException, InterruptedException {
        Boolean success = zmlpApp.getZmlpClient().deleteProject(projectId);

        assertEquals(true, success);
    }


}
