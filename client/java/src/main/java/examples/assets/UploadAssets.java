package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.AssetSpec;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetResponse;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class UploadAssets {

    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp(UUID.randomUUID().toString(), "PIXML-APIKEY");

        // Initialize AssetSpec List
        List<AssetSpec> assetSpecList =
                Arrays.asList(new AssetSpec("/Documents/1040.tiff"),
                        new AssetSpec("/Documents/1099-MISC.tiff"),
                        new AssetSpec("/Documents/W2.pdf"));

        // Upload Asset list
        BatchCreateAssetResponse response = zmlpApp.assets.uploadFiles(assetSpecList);

    }

}
