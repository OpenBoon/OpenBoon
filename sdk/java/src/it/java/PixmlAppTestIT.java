import com.fasterxml.jackson.databind.ObjectMapper;
import domain.*;
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
    public void testCreateProject() throws IOException, InterruptedException {

        ProjectSpec projectSpec = new ProjectSpec(projectName, projectId);
        Project project = pixmlApp.getPixmlClient().createProject(projectSpec);

        assertEquals(project.getId(), projectId);
        assertEquals(project.getName(), projectName);
    }


    @Test
    @Order(2)
    public void testFailOnCreateRepeatedApp() {

        ProjectSpec projectSpec = new ProjectSpec(projectName, projectId);

        assertThrows(IOException.class, () -> pixmlApp.getPixmlClient().createProject(projectSpec));
    }

    @Test
    @Order(3)
    public void testRetrieveProject() throws IOException, InterruptedException {

        List<UUID> uuidList = Arrays.asList(projectId);
        ProjectFilter projectFilter = new ProjectFilter(uuidList, null, null, null);

        Project project = pixmlApp.getPixmlClient().searchProject(projectFilter);

        assertEquals(project.getId(), projectId);
        assertEquals(project.getName(), projectName);
        assertEquals(project.getActorCreated(), "admin-key");
        assertEquals(project.getActorCreated(), "admin-key");
    }

    @Test
    @Order(4)
    public void testRetrieveAllProjects() throws IOException, InterruptedException {

        ProjectFilter projectFilter = new ProjectFilter(null, null, null, null);
        List<Project> projects = pixmlApp.getPixmlClient().getAllProjects(projectFilter);
        assertEquals(projects.get(0).getId(), projectId);
        assertEquals(projects.get(0).getName(), projectName);
    }

    @Test
    @Order(5)
    public void createDataSource() throws IOException, InterruptedException {

        Map value = new HashMap();

        value.put("name", "test");
        value.put("uri", "gs://test/test");

        dsTest = pixmlApp.getDataSourceApp().createDataSource("test", "gs://test/test", null, null, null);

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
        DataSource ds = pixmlApp.getDataSourceApp().getDataSource("test");

        assertEquals(value.get("name"), ds.getName());
        assertEquals(value.get("uri"), ds.getUri());

    }

    @Test
    @Order(7)
    public void importDataSource() throws IOException, InterruptedException {

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", dsTest.getId());
        DataSource ds = new DataSource(dataSourceParam);

        Map response = pixmlApp.getDataSourceApp().importDataSource(ds);

        assertEquals(response.get("dataSourceId"), dsTest.getId().toString());
        assert (((String) response.get("name")).contains(dsTest.getName()));
    }

    @Test
    @Order(8)
    public void updateCredentials() throws IOException, InterruptedException {

        Map status = this.pixmlApp.getDataSourceApp().updateCredentials(dsTest, "ABC123");

        Map dataSourceParam = new HashMap();
        dataSourceParam.put("id", projectId);
        DataSource ds = new DataSource(dataSourceParam);

        assertEquals(status.get("op"), "update");
        assertEquals(status.get("id"), dsTest.getId().toString());
    }

    @Test
    @Order(9)
    public void testDeleteApp() throws IOException, InterruptedException {
        Boolean success = pixmlApp.getPixmlClient().deleteProject(projectId);

        assertEquals(true, success);
    }


}
