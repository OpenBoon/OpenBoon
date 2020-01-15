package examples.assets;

import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.app.AssetApp;

import java.util.UUID;

public class AssetBase {

    public static AssetApp createAssetApp(){
        //Load ApiKey
        ApiKey key = new ApiKey(UUID.randomUUID().toString(), "PIXML-APIKEY");

        // Initialize AssetApp with default server URL
        AssetApp assetApp = new AssetApp(
                new ZmlpClient(key, null));

        return assetApp;
    }
}
