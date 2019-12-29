package com.zorroa.zmlp.sdk.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorroa.zmlp.sdk.ZmlpApp;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static com.zorroa.zmlp.sdk.Utils.updateEnvVariables;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("ZMLP App Test")
public class ZmlpAppTest {

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

        ZmlpApp zmlpApp = new ZmlpApp(keyDict, null);

        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }

    @DisplayName("Create app with Key String")
    @Test
    public void testCreateAppWithKeyString() {

        ZmlpApp zmlpApp = new ZmlpApp(keyString, null);

        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }

    @DisplayName("Create app with Key String.Byte[]")
    @Test
    public void testCreateAppWithKeyByteArr() {
        ZmlpApp zmlpApp = new ZmlpApp(keyStrByteArr, null);

        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }

    @DisplayName("Create app with ENV Variables")
    @Test
    public void testCreateAppFromEnv() throws ReflectiveOperationException {

        //Setup
        String server = "https://localhost:9999";

        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        updateEnvVariables("ZMLP_APIKEY", keyString);
        updateEnvVariables("ZMLP_SERVER", server);

        ZmlpApp zmlpApp = ZmlpApp.fromEnv();

        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }


    @DisplayName("Create app with ENV Variables")
    @Test
    public void testCreateAppFromEnvFile() throws ReflectiveOperationException {

        //Setup
        String server = "https://localhost:9999";

        //When
        // Setting up Environment Variables
        // It is valid just at this runtime
        updateEnvVariables("ZMLP_APIKEY_FILE", "src/test/resources/testkey.json");
        updateEnvVariables("ZMLP_SERVER", server);

        ZmlpApp zmlpApp = ZmlpApp.fromEnv();

        assertNotNull(zmlpApp);
        assertNotNull(zmlpApp.zmlpClient);
        assertTrue(zmlpApp.zmlpClient.isApiKeySet());
    }


}
