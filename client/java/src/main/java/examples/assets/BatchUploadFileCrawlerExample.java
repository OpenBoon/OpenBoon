package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetResponse;
import com.zorroa.zmlp.client.domain.asset.BatchUploadFileCrawler;

import java.io.IOException;

public class BatchUploadFileCrawlerExample {
    public static void main(String[] args) throws IOException {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        //Setup File Search requirements and Callback function to be executed after Batch upload
        BatchUploadFileCrawler batchUploadFileCrawler = new BatchUploadFileCrawler("./src/test/resources/")
                .addFileType("json") // OR
                .addMimeType("image/jpeg") // OR
                .setCallback(() -> System.out.println("Add an Function or a Runnable"));

        BatchCreateAssetResponse batchCreateAssetResponse = zmlpApp.assets.uploadFiles(batchUploadFileCrawler);

    }
}
