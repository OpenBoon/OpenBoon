package examples.assets;

import com.zorroa.zmlp.client.app.AssetApp;
import com.zorroa.zmlp.client.domain.asset.AssetSpec;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetResponse;

import java.util.Arrays;
import java.util.List;

public class UploadAssets extends AssetBase {

    public static void main(String[] args) {

        AssetApp assetApp = createAssetApp();

        // Initialize AssetSpec List
        List<AssetSpec> assetSpecList =
                Arrays.asList(new AssetSpec("/Documents/1040.tiff"),
                              new AssetSpec("/Documents/1099-MISC.tiff"),
                              new AssetSpec("/Documents/W2.pdf"));

        // Upload Asset list
        BatchCreateAssetResponse response = assetApp.uploadFiles(assetSpecList);

    }

}
