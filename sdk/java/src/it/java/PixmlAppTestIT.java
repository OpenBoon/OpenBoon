import domain.PixmlApp;
import org.junit.Before;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

import static domain.Utils.updateEnvVariables;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PixmlAppTestIT {

    PixmlApp pixmlApp;
    String server = "http://localhost:8080";

    String projectName = "ProjectTEST";
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-1111111111111");


    @BeforeEach
    public void setup() throws ReflectiveOperationException {
        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        updateEnvVariables("PIXML_APIKEY_FILE", "../../dev/config/keys/inception-key.json");
        updateEnvVariables("PIXML_SERVER", server);
        pixmlApp = new PixmlApp();
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


}
