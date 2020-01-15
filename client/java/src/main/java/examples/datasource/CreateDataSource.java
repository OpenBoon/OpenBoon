package examples.datasource;

import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.app.DataSourceApp;
import com.zorroa.zmlp.client.domain.datasource.DataSource;
import com.zorroa.zmlp.client.domain.datasource.DataSourceSpec;

import java.util.Arrays;

public class CreateDataSource {
    public static void main(String[] args) {

        //Load ApiKey
        ApiKey key = new ApiKey("abcd", "1234");

        // Initialize DataSourceApp with default server URL
        DataSourceApp dataSourceApp = new DataSourceApp(
                new ZmlpClient(key, null));

        DataSourceSpec dataSourceSpec = new DataSourceSpec()
                .setName("legal-files")
                .setUri("gs://my-legal-deparment/documents")
                .setFileTypes(Arrays.asList("tiff", "pdf"))
                .setAnalysis(Arrays.asList("zmlp-labels"));

        DataSource dataSource = dataSourceApp.createDataSource(dataSourceSpec);


    }
}
