package examples.datasource;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.datasource.DataSource;
import com.zorroa.zmlp.client.domain.datasource.DataSourceSpec;

import java.util.Arrays;
import java.util.UUID;

public class CreateDataSource {
    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp(UUID.randomUUID().toString(), "PIXML-APIKEY");

        DataSourceSpec dataSourceSpec = new DataSourceSpec()
                .setName("legal-files")
                .setUri("gs://my-legal-deparment/documents")
                .setFileTypes(Arrays.asList("tiff", "pdf"))
                .setAnalysis(Arrays.asList("zmlp-labels"));

        DataSource dataSource = zmlpApp.datasources.createDataSource(dataSourceSpec);
    }
}
