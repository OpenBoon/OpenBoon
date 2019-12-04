package domain;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Pixml Client Test")
public class PixmlAppTest {

    Map<String, String> keyDict = new HashMap<>();

    @Before
    public void setup() {
        keyDict.put("projectId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("keyId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("sharedKey", "test123test135");
    }

    @DisplayName("Create app with Dict")
    @Test
    public void testPixmlTestDict() {

        PixmlApp pixmlApp = new PixmlApp(keyDict, null);

        assertNotNull(pixmlApp);
        assertNotNull(pixmlApp.pixmlClient);
        assertNotNull(pixmlApp.pixmlClient.apiKey);
        assertNotNull(pixmlApp.pixmlClient.headers());

    }
}
