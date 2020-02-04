package examples.assets;

import com.zorroa.zmlp.client.ZmlpApp;
import com.zorroa.zmlp.client.domain.PagedList;
import com.zorroa.zmlp.client.domain.asset.Asset;
import com.zorroa.zmlp.client.domain.asset.AssetSearchResult;

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
        AssetSearchResult assetSearchResult = zmlpApp.assets.search(simpleElementQueryString);
        PagedList<Asset> searchResult = assetSearchResult.assets();

        for (Asset asset : searchResult)
            System.out.println(String.format("this is a cat: %s", asset.getAttr("source.path")));

    }
}
