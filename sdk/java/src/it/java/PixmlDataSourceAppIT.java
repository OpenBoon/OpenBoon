import domain.PixmlApp;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static domain.Utils.updateEnvVariables;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PixmlDataSourceAppIT {


    @DisplayName("Create app with ENV Variables")
    @Test
    public void testCreateAppFromEnvFile() throws ReflectiveOperationException, IOException, InterruptedException {

        //Setup
        String server = "https://localhost:8080";

        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        updateEnvVariables("PIXML_APIKEY_FILE", "/Users/ironaraujo/Projects/zorroa/zmlp/dev/config/keys/inception-key.json");
        updateEnvVariables("PIXML_SERVER", server);


        PixmlApp pixmlApp = new PixmlApp();


        Map requestParams = new HashMap();
        requestParams.put("name", "ProjectZERO");
        requestParams.put("projectId", UUID.fromString("00000000-0000-0000-0000-000000000000"));

        Map post = pixmlApp.getPixmlClient().post("/api/v1/projects", requestParams);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.getPixmlClient());
        assertNotNull(pixmlApp.getPixmlClient().getApiKey());
        assertNotNull(pixmlApp.getPixmlClient().headers());
    }


}
