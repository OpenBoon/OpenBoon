package examples.assets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zorroa.zmlp.client.ZmlpApp;

import java.util.Map;

public class DeleteQuery {
    public static void main(String[] args) throws JsonProcessingException {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        // Delete Query
        String deleteQuery =
                "{" +
                 "  \"query\": {" +
                 "    \"terms\": {" +
                 "      \"source.filename\": \"bob.jpg\"" +
                 "    }" +
                 "  }" +
                 "}";

        // Elastic Search Object as Map
        Map deleteES = zmlpApp.assets.deleteByQuery(deleteQuery);

    }
}
