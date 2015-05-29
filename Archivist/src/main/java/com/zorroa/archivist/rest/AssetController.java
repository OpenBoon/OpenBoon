package com.zorroa.archivist.rest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
public class AssetController {

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @RequestMapping(value="/assets/_search", method=RequestMethod.GET)
    public DeferredResult<String> search(@RequestBody String query) {
        SearchRequestBuilder builder = client.prepareSearch(alias)
                .setTypes("asset")
                .setSource(query);

        DeferredResult<String> result = new DeferredResult<String>();
        builder.execute(new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse response) {
                result.setResult(response.toString());
            }
            @Override
            public void onFailure(Throwable e) {
                result.setErrorResult(e);
            }
        });

        return result;
    }
}
