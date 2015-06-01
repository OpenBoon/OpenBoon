package com.zorroa.archivist.web;

import java.security.Principal;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
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
    public DeferredResult<String> search(@RequestBody String query, Principal principle) {
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

    @RequestMapping(value="/assets/_count", method=RequestMethod.GET)
    public DeferredResult<String> count(@RequestBody String query, Principal principle) {
        CountRequestBuilder builder = client.prepareCount(alias)
                .setTypes("asset")
                .setSource(query.getBytes());

        DeferredResult<String> result = new DeferredResult<String>();
        builder.execute(new ActionListener<CountResponse>() {
            @Override
            public void onResponse(CountResponse response) {
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
