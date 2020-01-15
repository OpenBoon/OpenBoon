package examples.datasource;

import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.app.DataSourceApp;

public class DataSourceBase {

    public static DataSourceApp createDataSourceApp() {
        //Load ApiKey
        ApiKey key = new ApiKey("abcd", "1234");

        // Initialize DataSourceApp with default server URL
        DataSourceApp dataSourceApp = new DataSourceApp(
                new ZmlpClient(key, null));

        return dataSourceApp;
    }
}
