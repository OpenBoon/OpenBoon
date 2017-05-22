package com.zorroa.common.config;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import static com.zorroa.common.config.NetworkEnvironment.ON_PREM;

/**
 * Created by chambers on 4/26/17.
 */
public class NetworkEnvironmentUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetworkEnvironmentUtils.class);

    public static NetworkEnvironment getNetworkEnvironment(String app, ApplicationProperties properties) {
        NetworkEnvironment env = new NetworkEnvironment();
        env.setApp(app);
        env.setPublicUri(NetworkEnvironmentUtils.getPublicUrl(properties));
        env.setPrivateUri(NetworkEnvironmentUtils.getPrivateUrl(properties));
        env.setLocation(NetworkEnvironmentUtils.getLocation(properties));
        env.setClusterPort(properties.getInt(app + ".cluster.command.port"));

        logger.info("--- NETWORK ENVIRONMENT ---");
        logger.info("Location: {}", env.getLocation());
        logger.info("External URI: {}", env.getPublicUri());
        logger.info("Internal URI: {}", env.getPrivateUri());
        logger.info("ClusterAddr: {}", env.getClusterAddr());
        return env;
    }

    public static String getLocation(ApplicationProperties properties) {
        if (properties.getBoolean("server.aws.check-location", true)) {
            Region region = Regions.getCurrentRegion();
            if (region == null) {
                return ON_PREM;
            } else {
                return region.getName();
            }
        } else {
            return ON_PREM;
        }
    }

    public static final String getPrivateHostname(ApplicationProperties properties) {
        String hostname = null;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignore1) {
            try {
                hostname = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignore2) {

            }
        }
        if (hostname == null) {
            throw new RuntimeException("Unable to determine public interface");
        }
        return hostname;

    }

    public static final String getPublicHostname(ApplicationProperties properties) {

        String hostname = properties.getString("server.fqdn", null);
        if (hostname != null) {
            return hostname;
        }

        hostname = properties.getString("server.address", null);
        if (hostname != null) {
            return hostname;
        }

        if (!getLocation(properties).equals(ON_PREM)) {

            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            requestBuilder = requestBuilder.setConnectTimeout(1000);
            requestBuilder = requestBuilder.setConnectionRequestTimeout(1000);

            try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(requestBuilder.build()).build()) {

                HttpResponse response = httpClient.execute(new HttpGet(
                        "http://169.254.169.254/latest/meta-data/public-ipv4"));
                HttpEntity entity = response.getEntity();
                hostname = EntityUtils.toString(entity, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException("AWS detected but unable to determine public interface", e);
            }
        }

        // Fall back on private hostname
        return getPrivateHostname(properties);
    }

    private static final URI getUrl(String host, int port, boolean ssl) {
        StringBuilder url = new StringBuilder(256);
        url.append("http");
        if (ssl) {
            url.append("s");
        }
        url.append("://");
        url.append(host);
        url.append(":");
        url.append(port);
        return URI.create(url.toString());
    }

    public static final URI getPrivateUrl(ApplicationProperties properties) {
        return getUrl(getPrivateHostname(properties),
                properties.getInt("server.port"),
                properties.getBoolean("server.ssl.enabled"));
    }

    public static final URI getPublicUrl(ApplicationProperties properties) {
        return getUrl(getPublicHostname(properties),
                properties.getInt("server.port"),
                properties.getBoolean("server.ssl.enabled"));
    }
}
