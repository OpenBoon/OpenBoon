package com.zorroa.common.cluster.client;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;

/**
 * Created by chambers on 5/9/17.
 */
public abstract class AbtractThriftClient implements Closeable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected TSocket socket = null;
    protected TTransport transport = null;
    protected TProtocol protocol = null;

    protected String host;
    protected int port;
    protected boolean connected = false;

    /**
     * Maximum # of retries before a ClusterConnectionException is thrown.
     * -1 to disable and try....forever.
     */
    private int maxRetries = -1;

    public AbtractThriftClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public AbtractThriftClient(String address) {
        address = convertUriToClusterAddr(address);
        String[] parts = address.split(":");
        this.host = parts[0];
        this.port = parts.length == 1 ? getDefaultPort() : Integer.valueOf(parts[1]);
    }

    public TProtocol connect() throws TException {
        if (!connected) {
            socket = new TSocket(host, port);
            socket.setConnectTimeout(10000);

            /**
             * TODO: deal with these.
             */
            //socket.setTimeout(10000);
            //socket.setSocketTimeout(5000);

            transport = new TFramedTransport(socket);
            protocol = new TCompactProtocol(transport);
            transport.open();
            connected = true;
        }
        return protocol;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public AbtractThriftClient setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    @Override
    public void close() {
        if (transport != null) {
            transport.close();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void backoff(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public abstract void reconnect() throws TException;

    public abstract int getDefaultPort();

    private final Object lock = new Object();

    private static final long backOffms = 100;

    private static final int logNthFailure = 10;

    public abstract class Reconnect<T> {
        private final boolean idempotent;

        Reconnect(boolean idempotent) {
            this.idempotent = idempotent;
        }

        Reconnect() {
            this.idempotent = false;
        }
        protected abstract T wrap() throws TException;

        public T execute() {
            int tryCount = 1;
            try {
                for (; ; ) {
                    try {
                        synchronized (lock) {
                            reconnect();
                            return wrap();
                        }
                    } catch (TTransportException e) {
                        backoff(Math.min(tryCount, logNthFailure) * backOffms);
                        if (tryCount == 1 || tryCount % logNthFailure == 0) {
                            logger.warn("{} FAILED to connect to {}:{} after {} tries, still retrying.",
                                    getClass(), host, port, tryCount);
                        }
                        if (tryCount >= maxRetries && maxRetries > 0) {
                            throw new ClusterConnectionException("Failed to connect to " +
                                    host + ":" + port + ", " + tryCount + " tries");
                        }
                        tryCount++;
                    } catch (ClusterException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new ClusterException(e);
                    }
                }
            } finally {
                if (tryCount > 1) {
                    logger.warn("{} RECONNECTED to {}:{} after {} tries.",
                            getClass(), host, port, tryCount);
                }
            }
        }
    }

    public static final String convertUriToClusterAddr(String uri) {
        /**
         * Backwards compatible with archivist 0.34
         */
        if (uri.startsWith("http")) {
            URI u = URI.create(uri);
            int port = u.getPort();
            if (port == 8066) {
                port = port-1;
            }
            return u.getHost().concat(":").concat(String.valueOf(port));
        }
        else {
            return uri;
        }
    }
}
