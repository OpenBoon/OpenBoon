package com.zorroa.archivist.web;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpSession;

import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @RequestMapping(value="/api/v1/assets/_search", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public void search(@RequestBody String query, HttpSession session, ServletOutputStream out) throws IOException {

        Room room = roomService.getActiveRoom(session.getId());
        roomService.sendToRoom(room, new Message(MessageType.ASSET_SEARCH, query));

        SearchRequestBuilder builder = client.prepareSearch(alias)
                .setTypes("asset")
                .setSource(query);

        SearchResponse response = builder.get();
        XContentBuilder content = XContentFactory.jsonBuilder(out);
        content.startObject();
        response.toXContent(content, ToXContent.EMPTY_PARAMS);
        content.endObject();
        content.close();
        out.close();
    }

    @RequestMapping(value="/api/v1/assets/_count", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public String count(@RequestBody String query, HttpSession session) {
        Room room = roomService.getActiveRoom(session.getId());
        roomService.sendToRoom(room, new Message(MessageType.ASSET_COUNT, query));

        CountRequestBuilder builder = client.prepareCount(alias)
                .setTypes("asset")
                .setSource(query.getBytes());

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

    @RequestMapping(value="/api/v1/assets/{id}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public void get(@PathVariable String id, HttpSession session, ServletOutputStream out) throws IOException {
        Room room = roomService.getActiveRoom(session.getId());
        roomService.sendToRoom(room, new Message(MessageType.ASSET_GET, id));

        GetResponse response = client.prepareGet(alias, "asset", id).get();
        XContentBuilder content = XContentFactory.jsonBuilder(out);
        content.startObject();
        response.toXContent(content, ToXContent.EMPTY_PARAMS);
        content.endObject();
        content.close();
        out.close();
    }
}
