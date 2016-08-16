package com.zorroa.common.cluster;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.HttpHost;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A software based HTTP load balancer.
 */
public class LoadBalancer {

    public enum Strategy {
        RoundRobin,
        Random,
        Never
    }

    private Strategy strategy = Strategy.RoundRobin;
    private final AtomicInteger requestCount = new AtomicInteger();

    private final List<HttpHost> hosts =  Lists.newArrayList();
    private final List<HttpHost> downHosts =  Lists.newArrayList();

    private final Map<Long, HttpHost> lastHost = Maps.newHashMap();

    public LoadBalancer() {
    }

    public LoadBalancer(Collection<String> addrs) {
        addHosts(addrs);
    }

    public LoadBalancer(String... addr) {
        for (String _addr: addr) {
            addHost(_addr);
        }
    }

    public boolean hasHosts() {
        return (hosts.size() + downHosts.size()) > 0;
    }

    public void addHosts(Collection<String> hosts) {
        for (String host: hosts) {
            addHost(host);
        }
    }

    public HttpHost addHost(String host) {
        URI uri = URI.create(host);
        return addHost(uri.getHost(), uri.getPort(), Protocol.valueOf(uri.getScheme()));
    }

    public HttpHost addHost(String host, int port, Protocol protocol) {
        HttpHost _host = new HttpHost(host, port, protocol.toString());
        hosts.add(_host);
        return _host;
    }

    public HttpHost lastHost() {
        HttpHost host = lastHost.get(Thread.currentThread().getId());
        if (host == null) {
            return null;
        }
        return new HttpHost(lastHost.get(Thread.currentThread().getId()));
    }

    public HttpHost nextHost() {
        if (hosts.isEmpty() && downHosts.isEmpty()) {
            throw new IllegalStateException("The load balancer has no hosts to balance!");
        }
        HttpHost result;
        for(;;) {
            if (hosts.size() == 1) {
                result = hosts.get(0);
                break;
            } else if (hosts.size() > 1) {
                int idx;
                switch (strategy) {
                    case RoundRobin:
                        idx = requestCount.incrementAndGet() % hosts.size();
                        break;
                    case Random:
                        idx = ThreadLocalRandom.current().nextInt(hosts.size());
                        break;
                    default:
                        idx = 0;
                }
                result = hosts.get(idx);
                break;
            } else {
                /*
                 * All all the hosts we marked as "down" back into the pool and
                 * start over again.
                 */
                hosts.addAll(downHosts);
            }
        }

        lastHost.put(Thread.currentThread().getId(), result);
        return result;
    }
}
