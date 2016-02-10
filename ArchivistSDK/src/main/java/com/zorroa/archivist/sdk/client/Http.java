package com.zorroa.archivist.sdk.client;

import com.zorroa.archivist.sdk.util.Json;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

/**
 * Created by chambers on 2/8/16.
 */
public class Http {

    public static <T> T post(HttpClient client, HttpHost host, String url, Object body, Class<T> resultType) {
        try {
            HttpPost post = new HttpPost(url);
            if (body != null) {
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new ByteArrayEntity(Json.serialize(body)));
            }
            HttpResponse response = client.execute(host, post);
            return Json.Mapper.readValue(response.getEntity().getContent(), resultType);
        } catch (Exception e) {
            ExceptionTranslator.translate(e);
        }

        return null;
    }

    public static void post(HttpClient client, HttpHost host, String url, Object body) {
        try {
            HttpPost post = new HttpPost(url);
            if (body != null) {
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new ByteArrayEntity(Json.serialize(body)));
            }
            HttpResponse response = client.execute(host, post);
        } catch (Exception e) {
            ExceptionTranslator.translate(e);
        }
    }
}
