package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.asset.BatchCreateAssetsResponse;
import com.zorroa.zmlp.client.domain.asset.BatchUploadFileCrawler;

import java.io.IOException;
import java.util.List;

public class BatchUploadFileCrawlerExample {
    public static void main(String[] args) throws IOException {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        //Setup File Search requirements and Callback function to be executed after Batch upload
        BatchUploadFileCrawler batchUploadFileCrawler = new BatchUploadFileCrawler("./src/test/resources/")
                .addFileType("json") // OR
                .addMimeType("image/jpeg"); // OR


        // Setting up callback function and Executing the upload.
        List<BatchCreateAssetsResponse> upload = batchUploadFileCrawler.upload(zmlpApp.assets, callback -> {
            System.out.printf("%s %s", callback.getBatchNumber(), callback.getFileCount());
        });
    }
}
