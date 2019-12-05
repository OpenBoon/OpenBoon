package domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;


import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@DisplayName("Pixml Client Test")
public class PixmlAppTest {

    Map<String, String> keyDict = new HashMap<>();
    byte[] keyStrByteArr;
    String keyString;
    ObjectMapper objectMapper = new ObjectMapper();


    @Before
    public void setup(){
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
    public void testCreateAppWithKeyString(){

        PixmlApp pixmlApp = new PixmlApp(keyString, null);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());
    }

    @DisplayName("Create app with Key String.Byte[]")
    @Test
    public void testCreateAppWithKeyByteArr(){
        PixmlApp pixmlApp = new PixmlApp(keyStrByteArr, null);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());
    }

    /*
    def test_create_app_from_env(self):
        server = "https://localhost:9999"
        os.environ['PIXML_APIKEY'] = self.key_str.decode()
        os.environ['PIXML_SERVER'] = server
        try:
            app1 = pixml.app.app_from_env()
            # Assert we can sign a request
            assert app1.client.headers()
            assert app1.client.server == server
        finally:
            del os.environ['PIXML_APIKEY']
            del os.environ['PIXML_SERVER']

     */

    @DisplayName("Create app with Key String.Byte[]")
    @Test
    public void testCreateAppFromEnv(){


        //Setup
        String server = "https://localhost:9999";
        System systemMock = Mockito.mock(System.class);

        //When
        when(System.getenv("PIXML_APIKEY")).thenReturn(keyString);
        when(System.getenv("PIXML_SERVER")).thenReturn(server);

        PixmlApp pixmlApp = new PixmlApp(keyStrByteArr, server);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());
    }


}
