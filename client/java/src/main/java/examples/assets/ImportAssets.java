package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.AssetCreateBuilder;
import com.zorroa.zmlp.client.domain.asset.AssetSpec;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetResponse;
import examples.ZmplUtil;

import java.util.UUID;

public class ImportAssets {
    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = ZmplUtil.createZmplApp(UUID.randomUUID(), "PIXML-APIKEY");

        // Setup import params
        AssetCreateBuilder assetCreateBuilder = new AssetCreateBuilder()
                .addAsset(new AssetSpec("gs://zorroa-dev-data/image/pluto.png"))
                .addAsset("gs://zorroa-dev-data/image/earth.png")
                .setAnalyze(false);

        // Import Assets
        BatchCreateAssetResponse batchCreateAssetResponse = zmlpApp.assets.importFiles(assetCreateBuilder);

    }
}
