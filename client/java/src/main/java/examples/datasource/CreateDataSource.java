package examples.datasource;

import com.zorroa.zmlp.client.app.DataSourceApp;
import com.zorroa.zmlp.client.domain.datasource.DataSource;
import com.zorroa.zmlp.client.domain.datasource.DataSourceSpec;

import java.util.Arrays;

public class CreateDataSource extends DataSourceBase {
    public static void main(String[] args) {

        DataSourceApp dataSourceApp = createDataSourceApp();

        DataSourceSpec dataSourceSpec = new DataSourceSpec()
                .setName("legal-files")
                .setUri("gs://my-legal-deparment/documents")
                .setFileTypes(Arrays.asList("tiff", "pdf"))
                .setAnalysis(Arrays.asList("zmlp-labels"));

        DataSource dataSource = dataSourceApp.createDataSource(dataSourceSpec);


    }
}
