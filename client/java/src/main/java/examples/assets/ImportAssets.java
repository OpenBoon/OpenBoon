package examples.assets;

import com.zorroa.zmlp.client.app.AssetApp;
import com.zorroa.zmlp.client.domain.asset.AssetCreateBuilder;
import com.zorroa.zmlp.client.domain.asset.AssetSpec;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetResponse;

public class ImportAssets extends AssetBase {
    public static void main(String[] args) {

        // Initialize AssetApp with default server URL
        AssetApp assetApp = createAssetApp();

        // Setup import params
        AssetCreateBuilder assetCreateBuilder = new AssetCreateBuilder()
                .addAsset(new AssetSpec("gs://zorroa-dev-data/image/pluto.png"))
                .addAsset("gs://zorroa-dev-data/image/earth.png")
                .setAnalyze(false);

        // Import Assets
        BatchCreateAssetResponse batchCreateAssetResponse = assetApp.importFiles(assetCreateBuilder);
    }
}
