package examples;

import com.zorroa.zmlp.client.ZmlpApp;

import java.util.UUID;

public class ZmplUtil {

    public static ZmlpApp createZmplApp(UUID uuid, String appKey) {
        //Load ApiKey
        ZmlpApp zmlpApp = new ZmlpApp(uuid.toString(), appKey);

        return zmlpApp;
    }

}
