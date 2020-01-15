package examples.assets;

import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.app.AssetApp;
import com.zorroa.zmlp.client.domain.asset.AssetSpec;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetResponse;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class UploadAssets {

    //Load ApiKey
    ApiKey key = new ApiKey(UUID.randomUUID().toString(), "1234");

    // Initialize AssetApp with default server URL
    AssetApp assetApp = new AssetApp(
            new ZmlpClient(key, null));


    // Initialize AssetSpec List
    List<AssetSpec> assetSpecList = Arrays.asList(new AssetSpec("src/test/resources/toucan.jpg"));

    // Upload Asset list
    BatchCreateAssetResponse response = assetApp.uploadFiles(assetSpecList);
}
