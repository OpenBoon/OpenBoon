package examples.assets;

import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.app.AssetApp;
import com.zorroa.zmlp.client.domain.asset.AssetCreateBuilder;
import com.zorroa.zmlp.client.domain.asset.AssetSpec;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetResponse;

import java.util.UUID;

public class ImportAssets {
    public static void main(String[] args) {

        //Load ApiKey
        ApiKey key = new ApiKey(UUID.randomUUID().toString(), "1234");

        // Initialize AssetApp with default server URL
        AssetApp assetApp = new AssetApp(
                new ZmlpClient(key, null));

        // Setup import params
        AssetCreateBuilder assetCreateBuilder = new AssetCreateBuilder()
                .addAsset(new AssetSpec("gs://zorroa-dev-data/image/pluto.png"))
                .addAsset("gs://zorroa-dev-data/image/earth.png")
                .setAnalyze(false);

        // Import Assets
        BatchCreateAssetResponse batchCreateAssetResponse = assetApp.importFiles(assetCreateBuilder);
    }
}
