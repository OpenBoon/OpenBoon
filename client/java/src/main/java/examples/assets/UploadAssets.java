package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.AssetSpec;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetsResponse;
import com.zorroa.zmlp.client.domain.asset.BatchUploadAssetsRequest;

public class UploadAssets {

    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        // Initialize AssetSpec Batch
        BatchUploadAssetsRequest batchAssetSpec = new BatchUploadAssetsRequest()
                .addAsset("/Documents/1040.tiff")
                .addAsset("/Documents/1099-MISC.tiff")
                //or Just AssetSpec URI
                .addAsset("/Documents/W2.pdf");

        // Upload BatchAssetSpec or List<AssetSpec>
        BatchCreateAssetsResponse response = zmlpApp.assets.uploadFiles(batchAssetSpec);
    }

}
