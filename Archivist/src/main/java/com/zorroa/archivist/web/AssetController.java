package com.zorroa.archivist.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.zorroa.archivist.Json;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import com.fasterxml.jackson.core.type.TypeReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.MessageType;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.service.RoomService;

@RestController
public class AssetController {

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    RoomService roomService;

    @RequestMapping(value="/api/v1/assets/_search", method=RequestMethod.GET)
    public void search(@RequestBody(required=false) String query, @RequestParam(value="q", required=false) String qstring, HttpSession session, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Room room = roomService.getActiveRoom(session.getId());
        roomService.sendToRoom(room, new Message(MessageType.ASSET_SEARCH, query));

        SearchRequestBuilder builder = client.prepareSearch(alias)
                .setTypes("asset");

        if (qstring != null) {
            builder.setQuery(QueryBuilders.queryStringQuery(qstring));
        }

        if (query != null) {
            builder.setSource(query.getBytes());
        }

        SearchResponse response = builder.get();
        OutputStream out = httpResponse.getOutputStream();
        XContentBuilder content = XContentFactory.jsonBuilder(out);
        content.startObject();
        response.toXContent(content, ToXContent.EMPTY_PARAMS);
        content.endObject();
        content.close();
        out.close();
    }

    @RequestMapping(value="/api/v1/assets/_count", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public String count(@RequestBody(required=false) String query, @RequestParam(value="q", required = false) String qstring, HttpSession session) {
        Room room = roomService.getActiveRoom(session.getId());
        roomService.sendToRoom(room, new Message(MessageType.ASSET_COUNT, query));

        CountRequestBuilder builder = client.prepareCount(alias)
                .setTypes("asset");

        if (qstring != null) {
            builder.setQuery(QueryBuilders.queryStringQuery(qstring));
        }
        else {
            builder.setSource(query.getBytes());
        }
        CountResponse response = builder.get();
        return new StringBuilder(128)
            .append("{\"count\":")
            .append(response.getCount())
            .append(",\"_shards\":{\"total\":")
            .append(response.getTotalShards())
            .append(",\"successful\":")
            .append(response.getSuccessfulShards())
            .append(",\"failed\":")
            .append(response.getFailedShards())
            .append("}}")
            .toString();
    }

    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.GET)
    public void get(@PathVariable String id, HttpSession session, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Room room = roomService.getActiveRoom(session.getId());
        roomService.sendToRoom(room, new Message(MessageType.ASSET_GET, id));

        GetResponse response = client.prepareGet(alias, "asset", id).get();
        OutputStream out = httpResponse.getOutputStream();

        XContentBuilder content = XContentFactory.jsonBuilder(out);
        content.startObject();
        response.toXContent(content, ToXContent.EMPTY_PARAMS);
        content.endObject();
        content.close();
        out.close();
    }

    @RequestMapping(value = "/api/v1/assets/{id}/_collections", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String update(@RequestBody String body, @PathVariable String id) throws Exception {
        // Add the request body array of collection names to the collections field
        String doc = "{\"collections\":" + body + "}";  // Hand-coded JSON doc update
        UpdateRequestBuilder builder = client.prepareUpdate(alias, "asset", id)
                .setDoc(doc)
                .setRefresh(true);  // Make sure we block until update is finished
        UpdateResponse response = builder.get();
        return new StringBuilder(128)
                .append("{\"created\":")
                .append(response.isCreated())
                .append(",\"version\":")
                .append(response.getVersion())
                .append("}")
                .toString();
    }
}
