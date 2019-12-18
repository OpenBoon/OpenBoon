import domain.DataSource;
import domain.DataSourceApp;
import domain.PixmlApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static domain.Utils.updateEnvVariables;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PixmlDataSourceAppIT {

    PixmlApp pixmlApp;
    String server = "http://localhost:8080";

    @Before
    public void setup() throws ReflectiveOperationException {
        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        updateEnvVariables("PIXML_APIKEY_FILE", "../../dev/config/keys/inception-key.json");
        updateEnvVariables("PIXML_SERVER", server);
        pixmlApp = new PixmlApp();
    }

    @DisplayName("Create app with ENV Variables")
    @Test
    public void testCreateAppFromEnvFile() throws IOException, InterruptedException {


        Map requestParams = new HashMap();
        requestParams.put("name", "ProjectZERO");
        requestParams.put("projectId", UUID.fromString("00000000-0000-0000-0000-000000000000"));

        Map post = pixmlApp.getPixmlClient().post("/api/v1/projects", requestParams);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.getPixmlClient());
        assertNotNull(pixmlApp.getPixmlClient().getApiKey());
        assertNotNull(pixmlApp.getPixmlClient().headers());

        assertEquals(post.get("id"), "00000000-0000-0000-0000-000000000000");
        assertEquals(post.get("name"), "ProjectZERO");

    }

    @Test
    public void testCreateRepeatedAppFromEnvFile() throws ReflectiveOperationException {


        Map requestParams = new HashMap();
        requestParams.put("name", "ProjectZERO");
        requestParams.put("projectId", UUID.fromString("00000000-0000-0000-0000-000000000000"));

        assertThrows(IOException.class, () -> pixmlApp.getPixmlClient().post("/api/v1/projects", requestParams));

    }

    @Test
    public void getDataSource() throws IOException, InterruptedException, ReflectiveOperationException {
        this.setup();
        DataSourceApp dataSourceApp = pixmlApp.getDataSourceApp();
        DataSource ds = dataSourceApp.getDataSource("ProjectZERO");
        int a = 0;

    }


}
