package com.zorroa.zmlp.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.zorroa.zmlp.sdk.TestUtils.updateEnvVariables;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("ZMLP App Test")
public class ZmlpAppTest {

    Map<String, String> keyDict = new HashMap<>();
    String base64Key;

    @Before
    public void setup() throws JsonProcessingException {
        keyDict.put("projectId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("keyId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("sharedKey", "test123test135");

        base64Key = Base64.encodeBase64String(Json.mapper.writeValueAsBytes(keyDict));
    }

    @DisplayName("Create app with Base64 ApiKey")
    @Test
    public void testCreateAppWithBase64Key() {
        System.out.println(base64Key);

        ZmlpApp zmlpApp = new ZmlpApp(base64Key, null);
        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }

    @DisplayName("Create app with JSON file ApiKey")
    @Test
    public void testCreateAppWithJsonFileKey() {
        File key = new File("src/test/resources/testkey.json");
        ZmlpApp zmlpApp = new ZmlpApp(key, null);
        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }

    @DisplayName("Create app with Base64 APIKey in environment")
    @Test
    public void testCreateAppFromEnv() {
        updateEnvVariables("ZMLP_APIKEY", base64Key);
        updateEnvVariables("ZMLP_SERVER", "https://localhost:9999");

        ZmlpApp zmlpApp = ZmlpApp.fromEnv();

        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }

    @DisplayName("Create app with ENV Variables")
    @Test
    public void testCreateAppFromEnvFile() throws ReflectiveOperationException {
        updateEnvVariables("ZMLP_APIKEY_FILE", "src/test/resources/testkey.json");
        updateEnvVariables("ZMLP_SERVER",  "https://localhost:9999");

        ZmlpApp zmlpApp = ZmlpApp.fromEnv();
        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }
}
