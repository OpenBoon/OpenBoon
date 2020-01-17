package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.PagedList;
import com.zorroa.zmlp.client.domain.asset.Asset;

import java.util.HashMap;
import java.util.Map;

public class SearchElement {

    public static void main(String[] args) {

        // Initialize ZmlpApp
        ZmlpApp zmlpApp = new ZmlpApp("PIXML-APIKEY", "Server URL or Null for Default");

        //Create Query String
        Map simpleElementQueryString = new HashMap();
        Map query = new HashMap();
        query.put("query", "persian | angora");
        simpleElementQueryString.put("simple_query_string", query);

        //Search Asset
        PagedList<Asset> searchResult = zmlpApp.assets.search(simpleElementQueryString);

        for (Asset asset : searchResult)
            System.out.println(String.format("this is a cat: %s", asset.getAttr("source.path")));

    }
}
