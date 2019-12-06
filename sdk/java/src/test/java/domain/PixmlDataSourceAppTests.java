package domain;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PixmlDataSourceAppTests {

    Map keyDict;
    PixmlApp app;

    @Before
    public void setup() {
        //This is not a valid key
        keyDict = new HashMap();
        keyDict.put("projectId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("keyId", "A5BAFAAA-42FD-45BE-9FA2-92670AB4DA80");
        keyDict.put("sharedKey", "test123test135");

        app = new PixmlApp(keyDict);
    }

    @Test
    public void createDataSource(){}

    @Test
    public void getDataSource(){}

    @Test
    public void importDataSource(){}

    @Test
    public void updateCredentials(){}

}
