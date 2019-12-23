package domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Pixml Client Test")
public class PixmlAppTest {

    Map<String, String> keyDict = new HashMap<>();
    byte[] keyStrByteArr;
    String keyString;
    ObjectMapper objectMapper = new ObjectMapper();


    @Before
    public void setup() {
        keyDict.put("projectId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("keyId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("sharedKey", "test123test135");

        try {
            keyString = objectMapper.writeValueAsString(keyDict);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
        keyStrByteArr = Base64.encodeBase64(keyString.getBytes());
    }

    @DisplayName("Create app with Dict")
    @Test
    public void testCreateAppWithKeyDict() {

        PixmlApp pixmlApp = new PixmlApp(keyDict, null);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());

    }

    @DisplayName("Create app with Key String")
    @Test
    public void testCreateAppWithKeyString() {

        PixmlApp pixmlApp = new PixmlApp(keyString, null);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());
    }

    @DisplayName("Create app with Key String.Byte[]")
    @Test
    public void testCreateAppWithKeyByteArr() {
        PixmlApp pixmlApp = new PixmlApp(keyStrByteArr, null);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());
    }

    @DisplayName("Create app with ENV Variables")
    @Test
    public void testCreateAppFromEnv() throws ReflectiveOperationException {

        //Setup
        String server = "https://localhost:9999";

        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        updateEnv("PIXML_APIKEY", keyString);
        updateEnv("PIXML_SERVER", server);

        PixmlApp pixmlApp = new PixmlApp();

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());
    }


    @DisplayName("Create app with ENV Variables")
    @Test
    public void testCreateAppFromEnvFile() throws ReflectiveOperationException {

        //Setup
        String server = "https://localhost:9999";

        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        updateEnv("PIXML_APIKEY_FILE", "src/test/resources/testkey.json");
        updateEnv("PIXML_SERVER", server);

        PixmlApp pixmlApp = new PixmlApp();

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());
    }



    private void updateEnv(String name, String val) throws ReflectiveOperationException {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(name, val);
    }


}
