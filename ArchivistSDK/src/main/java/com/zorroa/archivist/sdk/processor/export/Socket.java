package com.zorroa.archivist.sdk.processor.export;

/**
 * A Socket is a receptable that accepts a @see "com.zorroa.archivist.sdk.processor.export.Cord"
 *
 * Data coming into a @see "com.zorroa.archivist.sdk.processor.export.Port always arrives via
 * a Socket
 */
public class Socket<T> {

    private Port<T> port;
    private Cord cord;

    public Socket(Port<T> port) {
        this.port = port;
    }

    /**
     * Plugs the given Cord into this Socket.
     *
     * @param cord
     */
    public void connect(Cord cord) {
        if (cord == this.cord) { return; }
        this.cord = cord;
        cord.connect(this);
    }

    /**
     * Return the Cord plugged into this socket.
     *
     * @return
     */
    public Cord<T> getCord() {
        return cord;
    }

    /**
     * Get the Port that this Socket is on.
     *
     * @return
     */
    public Port<T> getPort() {
        return port;
    }
}
